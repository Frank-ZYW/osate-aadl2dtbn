package org.osate2.bayes.dtbn.Util;

import java.util.Objects;

/** Pair **/
public class Pair<T> {

    public T u1;
    public T u2;

    public Pair(T u1, T u2) {
        this.u1 = u1;
        this.u2 = u2;
    }

    /** Pair equal definition **/
    @Override
    public boolean equals(Object o){
        if(o instanceof Pair<?> pair) {
            return this.u1 == pair.u1 && this.u2 == pair.u2;
        }
        return false;
    }

    /** Pair hashKey **/
    @Override
    public int hashCode() {
        return Objects.hash(this.u1, this.u2);
    }

}
