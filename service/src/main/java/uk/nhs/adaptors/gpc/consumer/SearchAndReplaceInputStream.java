package uk.nhs.adaptors.gpc.consumer;

import java.io.IOException;
import java.io.InputStream;

public class SearchAndReplaceInputStream extends InputStream {
    private InputStream is;
    private char[] search;
    private char[] replace;

    private int len, pos, idx;
    private char ch, buf[];

    public SearchAndReplaceInputStream(InputStream is, String search, String replace) {
        this.is = is;
        this.search = search.toCharArray();
        this.replace = replace.toCharArray();

        len = this.search.length;
        pos = 0;
        idx = -1;

        ch = this.search[0];
        buf = new char[Math.max(this.search.length, this.replace.length)];
    }

    @Override
    public int read() throws IOException {
        if ( idx == -1 ) {
            idx = 0;

            int i = -1;
            while ( ( i = is.read() ) != -1 && ( buf[pos] = (char)i ) == ch ) {
                if ( ++pos == len ) {
                    break;
                }

                ch = search[pos];
            }

            if ( pos == len ) {
                buf = new char[Math.max(this.search.length, this.replace.length)];
                System.arraycopy(replace, 0, buf, 0, replace.length);
            }

            pos = 0;
            ch = search[pos];
        }

        int toReturn = -1;
        if ( idx > -1 && buf[idx] != 0 ) {
            toReturn = buf[idx];
            buf[idx] = 0;
            if ( idx < buf.length - 1 && buf[idx + 1] != 0 ) {
                idx++;
            } else {
                idx = -1;
                buf = new char[Math.max(this.search.length, this.replace.length)];
            }
        }

        return toReturn;
    }
}