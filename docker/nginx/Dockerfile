FROM nginx

COPY ./nginx.conf /etc/nginx/conf.d/default.conf

COPY ./certificates/server_public.crt /etc/ssl/server_public.crt

COPY ./certificates/server_private.key /etc/ssl/server_private.key

EXPOSE 443