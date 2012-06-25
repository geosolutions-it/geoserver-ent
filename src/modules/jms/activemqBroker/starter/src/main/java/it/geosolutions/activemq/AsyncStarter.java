package it.geosolutions.activemq;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class AsyncStarter {
    
    
    
//    public void asyncStart() {
//        executorService.submit(new Callable<Object>() {
//
//            @Override
//            public Object call() throws Exception {
//                broker.start();
//                return null;
//            }
//            
//        });
//    }
    
    public AsyncStarter(final ExecutorService executorService, final org.apache.activemq.xbean.XBeanBrokerService broker){
        executorService.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                broker.start();
                return null;
            }
            
        });
    }
    
}
