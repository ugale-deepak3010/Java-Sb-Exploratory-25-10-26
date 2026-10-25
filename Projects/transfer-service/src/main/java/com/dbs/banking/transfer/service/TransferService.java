package com.dbs.banking.transfer.service;

import com.dbs.banking.common.dto.TransferRequest;
import com.dbs.banking.common.dto.TransferResponse;
import com.dbs.banking.common.enums.TransferStatus;

public interface TransferService {
    TransferResponse initiateTransfer(TransferRequest request);
    TransferResponse getTransfer(String transferId);
    void updateTransferStatus(String transferId, TransferStatus status, String reason);
}
