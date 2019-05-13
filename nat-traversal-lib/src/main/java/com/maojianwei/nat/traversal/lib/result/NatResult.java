package com.maojianwei.nat.traversal.lib.result;

public abstract class NatResult {

    private int errCode;

    // for deserializer
    protected NatResult() {
        errCode = Integer.MIN_VALUE;
    }

    // for serializer, and for App usage
    public NatResult(int errCode) {
        this.errCode = errCode;
    }

    // for deserializer
    public void setErrCode(int errCode) {
        this.errCode = errCode;
    }

    // for serializer, and for App usage
    public int getErrCode() {
        return errCode;
    }
}
