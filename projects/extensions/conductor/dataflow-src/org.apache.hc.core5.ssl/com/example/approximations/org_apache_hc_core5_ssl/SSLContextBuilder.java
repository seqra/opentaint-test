package com.example.approximations.org_apache_hc_core5_ssl;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.security.cert.X509Certificate;

/**
 * Dataflow approximation for org.apache.hc.core5.ssl.SSLContextBuilder.
 *
 * loadTrustMaterial(TrustStrategy) registers a TrustStrategy callback on the
 * builder. TrustStrategy is a single-abstract-method functional interface:
 *
 *     boolean isTrusted(X509Certificate[] chain, String authType)
 *
 * The builder retains the strategy and invokes it later (during the TLS
 * handshake the SSLContext drives). Without the approximation the analyzer never
 * sees the callback invoked, so any flow inside the lambda body — or data the
 * lambda captures and writes into state read back afterwards — is dropped. The
 * approximation runs the callback against fresh (empty) handshake arguments (the
 * callback boundary) so taint the lambda carries is analyzed. loadTrustMaterial
 * returns the builder for chaining, so return `self`.
 */
@Approximate(org.apache.hc.core5.ssl.SSLContextBuilder.class)
public class SSLContextBuilder {

    public org.apache.hc.core5.ssl.SSLContextBuilder loadTrustMaterial(
            @ArgumentTypeContext org.apache.hc.core5.ssl.TrustStrategy trustStrategy)
            throws Throwable {
        org.apache.hc.core5.ssl.SSLContextBuilder self =
                (org.apache.hc.core5.ssl.SSLContextBuilder) (Object) this;
        if (OpentaintNdUtil.nextBool()) {
            trustStrategy.isTrusted(new X509Certificate[0], "");
        }
        return self;
    }
}
