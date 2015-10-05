package com.iftekhar.volleyplus;

import com.android.volley.Request;
import com.android.volley.VolleyError;

import java.util.LinkedList;

/**
 * @author Iftekhar Ahmed
 */

/**
 * Wrapper class to map a {@link Request} to the set of active {@link DataContainer} objects that are
 * interested in its results.
 * @param <T> The type of data to be requested
 */
public class BatchedRequest<T> {
    /**
     * The request being tracked
     */
    protected final Request<T> mRequest;

    /**
     * The result of the request being tracked by this item
     */
    protected T mResponseData;

    /**
     * Error if one occurred for this response
     */
    protected VolleyError mError;

    /**
     * List of all of the active DataContainers that are interested in the request
     */
    protected final LinkedList<DataContainer<T>> mContainers = new LinkedList<>();

    /**
     * Constructs a new BatchedRequest object
     *
     * @param request   The request being tracked
     * @param container The DataContainer of the person who initiated the request.
     */
    public BatchedRequest(Request<T> request, DataContainer<T> container) {
        mRequest = request;
        mContainers.add(container);
    }

    /**
     * Set the error for this response
     */
    public void setError(VolleyError error) {
        mError = error;
    }

    /**
     * Get the error for this response
     */
    public VolleyError getError() {
        return mError;
    }

    /**
     * Adds another DataContainer to the list of those interested in the results of
     * the request.
     */
    public void addContainer(DataContainer<T> container) {
        mContainers.add(container);
    }

    /**
     * Detaches the DataContainer from the request and cancels the request if no one is
     * left listening.
     *
     * @param container The container to remove from the list
     * @return True if the request was canceled, false otherwise.
     */
    public boolean removeContainerAndCancelIfNecessary(DataContainer<T> container) {
        mContainers.remove(container);
        if (mContainers.size() == 0) {
            mRequest.cancel();
            return true;
        }
        return false;
    }
}