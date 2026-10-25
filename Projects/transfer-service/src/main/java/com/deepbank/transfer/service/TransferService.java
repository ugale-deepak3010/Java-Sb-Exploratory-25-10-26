package com.deepbank.transfer.service;

import com.deepbank.common.dto.TransferRequest;
import com.deepbank.common.dto.TransferResponse;
import com.deepbank.common.enums.TransferStatus;

public interface TransferService {
    TransferResponse initiateTransfer(TransferRequest request);
    TransferResponse getTransfer(String transferId);
    void updateTransferStatus(String transferId, TransferStatus status, String reason);
}
