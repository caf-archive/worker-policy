/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cafdataprocessing.worker.policy.common;

import com.hpe.caf.util.ref.DataSource;
import com.hpe.caf.util.ref.DataSourceException;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.util.ref.SourceNotFoundException;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapper class which can wrap the DataStore reference field, and only return the InputStream which represents
 * its content, when it is actually requested. e.g. by a call to read().
 */
public class DataStoreAwareInputStream extends InputStream {

    private InputStream streamForDataStore;
    private ReferencedData referencedData;
    private DataSource dataSource;
    private boolean notAtStartOfStream = false;

    private static Logger logger = LoggerFactory.getLogger(DataStoreAwareInputStream.class);

    public DataStoreAwareInputStream(ReferencedData referencedData, DataSource dataSource) {
        Validate.notNull(referencedData);

        this.referencedData = referencedData;
        this.dataSource = dataSource;
    }

    // We need to implement the closable interface, so we can in turn close the input
    // stream if its been opened.
    public void close() throws IOException {
        if (streamForDataStore == null)
            return;

        streamForDataStore.close();
        streamForDataStore = null;
    }

    private synchronized void checkGetDataStream() {
        // if we have calling this we need to ensure the input stream has been returned
        // by the Datastore.
        if (streamForDataStore != null) {
            // we have it already.
            return;
        }

        try {
            streamForDataStore = referencedData.acquire(dataSource);
        } catch (SourceNotFoundException e) {
            throw new RuntimeException(e);
        } catch (DataSourceException e) {
            throw new RuntimeException(e);
        }
    }

    // InputStream interface wrapping...
    @Override
    public int read() throws IOException {
        checkGetDataStream();

        notAtStartOfStream = true;
        return streamForDataStore.read();
    }

    @Override
    public int read(byte b[]) throws IOException {
        checkGetDataStream();

        notAtStartOfStream = true;
        return streamForDataStore.read(b);
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        checkGetDataStream();

        notAtStartOfStream = true;
        return streamForDataStore.read(b, off, len);
    }


    public long skip(long n) throws IOException {

        checkGetDataStream();

        notAtStartOfStream = true;
        return streamForDataStore.skip(n);
    }

    public int available() throws IOException {
        checkGetDataStream();

        return streamForDataStore.available();
    }

    public synchronized void mark(int readlimit) {
        checkGetDataStream();

        streamForDataStore.mark(readlimit);
    }

    public synchronized void reset() throws IOException {
        checkGetDataStream();

        // Now certain inputstreams, dont support reset of the information.
        // As such we need to emulate this ourselves.  Its either that, or
        // we need to read the entire stream once, into either a memory stream which supports it,
        // or a string, each with their own drawbacks.
        // For now I am going with closing the existin stream, and re-establishing it with the datastore.
        if (streamForDataStore.markSupported()) {
            streamForDataStore.reset();
            notAtStartOfStream = true;
            return;
        }

        // close stream, and get it again, only if we have actually changed it somehow
        // to no longer be at the start of the stream!
        if (notAtStartOfStream == false) {
            // we haven't yet touched the stream, so just exit.
            return;
        }

        // close the current stream, and re-aquire it at the start.
        streamForDataStore.close();
        streamForDataStore = null;
        checkGetDataStream();
    }

    public boolean markSupported() {
        checkGetDataStream();

        return streamForDataStore.markSupported();
    }
}
