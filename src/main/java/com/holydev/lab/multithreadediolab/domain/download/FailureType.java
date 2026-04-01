package com.holydev.lab.multithreadediolab.domain.download;

public enum FailureType {
    INVALID_RESPONSE,
    CONNECT_TIMEOUT,
    READ_TIMEOUT,
    HTTP_ERROR,
    IO_WRITE_ERROR,
    NETWORK_ERROR,
    UNKNOWN
}
