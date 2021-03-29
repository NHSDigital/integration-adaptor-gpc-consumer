package uk.nhs.adaptors.gpc.consumer.filters;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;

import reactor.core.CoreSubscriber;
import reactor.core.Fuseable;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

@Configuration
public class ReactorMdcSupportDecorator {

    private static final String MDC_HOOK_KEY = "MDC";
    private static final Function<? super Publisher<Object>, ? extends Publisher<Object>> LIFTER =
        Operators.liftPublisher(
            publisher -> MDC.getCopyOfContextMap() != null
                    && !(publisher instanceof Fuseable.ScalarCallable),
            (publisher, coreSubscriber) -> new MdcPropagatingSubscriber<>(coreSubscriber)
        );

    @PostConstruct
    public void contextOperatorHook() {
        Hooks.onEachOperator(MDC_HOOK_KEY, LIFTER);
    }

    @PreDestroy
    public void cleanupHook() {
        Hooks.resetOnEachOperator(MDC_HOOK_KEY);
    }

    static class MdcPropagatingSubscriber<T> implements CoreSubscriber<T> {
        static final String MDC_CONTEXT_KEY = "mdc-context";

        private final CoreSubscriber<T> delegate;
        private final Context context;

        MdcPropagatingSubscriber(CoreSubscriber<T> delegate) {
            this.delegate = delegate;
            Context currentContext = this.delegate.currentContext();
            this.context = currentContext.hasKey(MDC_CONTEXT_KEY)
                ? currentContext
                : currentContext.put(MDC_CONTEXT_KEY, new HashMap<>(MDC.getCopyOfContextMap()));
        }

        @Override
        public void onSubscribe(Subscription s) {
            this.delegate.onSubscribe(s);
        }

        @Override
        public void onNext(T t) {
            Map<String, String> map = this.context.get(MDC_CONTEXT_KEY);
            MDC.setContextMap(map);

            this.delegate.onNext(t);
        }

        @Override
        public void onError(Throwable t) {
            this.delegate.onError(t);
        }

        @Override
        public void onComplete() {
            MDC.clear();
            this.delegate.onComplete();
        }

        @Override
        public Context currentContext() {
            return this.context;
        }
    }

}