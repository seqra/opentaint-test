package com.example.approximations.org_apache_http_ssl;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

import java.security.KeyStore;
import java.security.cert.X509Certificate;

/**
 * Dataflow approximation for org.apache.http.ssl.SSLContextBuilder
 * (Apache HttpClient/HttpCore 4.x).
 *
 * loadTrustMaterial(KeyStore, TrustStrategy) registers a trust callback. At
 * certificate-verification time HttpCore invokes the strategy's
 *   boolean isTrusted(X509Certificate[] chain, String authType)
 * passing the certificate chain that is checked against the supplied trust
 * material (the KeyStore). To keep taint flowing through the registered
 * callback for completeness, model the registration by invoking the strategy
 * with the KeyStore-derived chain so taint on the trust material reaches the
 * callback body, and return the builder (self) so the fluent chain keeps any
 * existing taint.
 */
@Approximate(org.apache.http.ssl.SSLContextBuilder.class)
public class SSLContextBuilder {

    public org.apache.http.ssl.SSLContextBuilder loadTrustMaterial(
            KeyStore trustStore,
            @ArgumentTypeContext org.apache.http.ssl.TrustStrategy trustStrategy) throws Throwable {
        org.apache.http.ssl.SSLContextBuilder self =
                (org.apache.http.ssl.SSLContextBuilder) (Object) this;
        if (trustStrategy != null && OpentaintNdUtil.nextBool()) {
            // The chain checked by the callback is derived from the trust
            // material; route the KeyStore's taint into the callback.
            X509Certificate[] chain = (X509Certificate[]) (Object) trustStore;
            trustStrategy.isTrusted(chain, "");
        }
        return self;
    }
}
