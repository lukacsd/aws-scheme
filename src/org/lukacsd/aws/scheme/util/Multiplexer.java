/*
 * Copyright 2014 David Lukacs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lukacsd.aws.scheme.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Multiplexer {
    private CountDownLatch doneSignal;
    private Collection<Runnable> tasks = new LinkedList<Runnable>( );
    private volatile TaskFailedException lastException;

    public Multiplexer task( final Runnable task ) {
        tasks.add( new Runnable( ) {

            @Override
            public void run() {
                try {
                    task.run( );
                } catch ( Exception ex ) {
                    Multiplexer.this.lastException = new TaskFailedException( ex );
                } finally {
                    doneSignal.countDown( );
                }
            }
        } );

        return this;
    }

    public void executeAndBlock() {
        if ( tasks.size( ) > 0 ) {
            ExecutorService pool = Executors.newFixedThreadPool( tasks.size( ) );
            doneSignal = new CountDownLatch( tasks.size( ) );

            for ( Runnable task : tasks ) {
                pool.execute( task );
            }

            while ( true ) {
                try {
                    doneSignal.await( );

                    pool.shutdown( );

                    break;
                } catch ( InterruptedException e ) {
                }
            }

            rethrowLastException( );
        }
    }

    private void rethrowLastException() {
        if ( lastException != null ) {
            throw lastException;
        }
    }

    public class TaskFailedException extends RuntimeException {
        private static final long serialVersionUID = -982672692098911597L;

        public TaskFailedException( Throwable throwable ) {
            super( throwable );
        }
    }

}
