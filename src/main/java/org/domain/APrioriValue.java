package org.domain;

/**
 * Class that represents the range that a variable could have during the experiments.
 * <p>
 *     <b>i.e.</b>: <br>
 *         APrioriValue revenue = new APrioriValue(MIN_REVENUE, MAX_REVENUE). <br>
 *         So, revenue = [MIN_REVENUE, MAX_REVENUE].
 * </p>
 *
 * @author Leonardo Benitez.
 */
public class APrioriValue {

    private Float minValue;
    private Float maxValue;

    public APrioriValue(Float minValue, Float maxValue){
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /* Getters and Setters */

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
