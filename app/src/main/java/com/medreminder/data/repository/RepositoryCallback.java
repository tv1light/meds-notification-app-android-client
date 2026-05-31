package com.medreminder.data.repository;

public interface RepositoryCallback<T> {
    void onSuccess(T value);

    void onError(String message);
}
