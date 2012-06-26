package it.geosolutions.activemq;

public interface AsyncServiceMonitor {
    public boolean isStarted();
    public boolean isSlave();
    public void asyncStart() throws Exception;
    public void asyncStop() throws Exception;
}
