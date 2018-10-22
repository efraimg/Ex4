package com.lightricks.efraim.ex4.util;

public interface Observer<ObservedType> {
    public void update(ObservedType data);
}