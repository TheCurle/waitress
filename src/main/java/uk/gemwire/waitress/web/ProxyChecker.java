package uk.gemwire.waitress.web;

import uk.gemwire.waitress.Waitress;
import uk.gemwire.waitress.config.Config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ProxyChecker extends Thread{
    boolean isProxyAlive = false;
    Runnable callback;

    public ProxyChecker(Runnable callback) {
        this.callback = callback;
        this.setName("proxy checker");
    }

    @Override
    public void run() {
        try {
            isProxyAlive = isAvailable(Config.PROXY_REPO);
            if (!isProxyAlive) Waitress.LOGGER.warn("Proxy is not alive!");
            Thread.sleep(1000 * 60 * 5); // Sleep for 5 minutes
        } catch (InterruptedException e) {
            callback.run();
            throw new RuntimeException(e);
        }
    }

    public boolean isAvailable(String address) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestMethod("HEAD");
            int responseCode = connection.getResponseCode();
            return (200 <= responseCode && responseCode <= 399);
        } catch (IOException exception) {
            return false;
        }
    }
}
