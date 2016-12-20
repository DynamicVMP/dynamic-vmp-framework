package org.domain;

/**
 * Created by Leonardo Benitez.
 */
public class APrioriValue {

    private Float minValue;
    private Float maxValue;

    public APrioriValue(Float minValue, Float maxValue){
        this.minValue = minValue;
        this.maxValue = maxValue;
    }


    public Float getMinValue() {
        return minValue;
    }

    public void setMinValue(Float minValue) {
        this.minValue = minValue;
    }

    public Float getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Float maxValue) {
        this.maxValue = maxValue;
    }
}
