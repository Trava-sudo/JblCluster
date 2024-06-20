package hub.ebb.jblcluster.eventservice.model;

import hub.jbl.common.lib.date.DateUtils;
import hub.jbl.common.lib.date.JblDateTime;

import java.io.Serializable;

/**
 * Created by Stefano.Coletta on 07/11/2016.
 */
public class JpsSequenceNumber implements Serializable {

    JblDateTime dateTime;
    private int counter;

    public JpsSequenceNumber() {

    }

    public JpsSequenceNumber(Long seqTs,

                             Integer seqGMT, Integer seqCounter) {

        dateTime = new JblDateTime().withTimestamp(seqTs).withGmt(seqGMT);
        counter = seqCounter;
    }

    public JblDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(JblDateTime dateTime) {
        this.dateTime = dateTime;
    }


    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JpsSequenceNumber)) return false;

        JpsSequenceNumber that = (JpsSequenceNumber) o;

        if (counter != that.counter) return false;
        return dateTime != null ? dateTime.equals(that.dateTime) : that.dateTime == null;

    }

    @Override
    public String toString() {
        return "JpsSequenceNumber{" +
                "dateTime=" + dateTime +
                ", counter=" + counter +
                '}';
    }

    @Override
    public int hashCode() {
        int result = dateTime != null ? dateTime.hashCode() : 0;
        result = 31 * result + counter;


        return result;
    }

    public static JpsSequenceNumber Now() {
        int counter = (int) DateUtils.getUnixTSInMillis() + new Double(Math.random() * 10000).intValue();
        return new Builder().withTimestamp(DateUtils.getUnixTSInMillis()).withGmt(DateUtils.getGMTInMinutes()).withCounter(counter).build();

    }


    public static class Builder {

        private final JpsSequenceNumber entry;

        public Builder() {
            entry = new JpsSequenceNumber();
            entry.setDateTime(new JblDateTime());
        }


        public Builder withTimestamp(long ts) {
            entry.getDateTime().setTimestamp(ts);
            return this;
        }

        public Builder withGmt(int gmt) {
            entry.getDateTime().setGmt(gmt);
            return this;
        }

        public Builder withCounter(int counter) {
            entry.setCounter(counter);
            return this;
        }

        public JpsSequenceNumber build() {
            return entry;
        }


    }


}
