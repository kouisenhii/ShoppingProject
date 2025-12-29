package com.tw.shopping.main.exception;

// 定義一個專門用來處理庫存不足的異常
public class StockNotEnoughException extends RuntimeException{
    public StockNotEnoughException (String message){
        super(message);
    }
}
