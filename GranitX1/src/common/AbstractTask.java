package common;

/**
 * An abstract class, provides common functionality for AbstractClient tasks.
 * @author kamyshev.a
 */
public abstract class AbstractTask {

    public enum StatusEnum {
        prepared, requested, loaded, exspired
    }

    private long time;
    protected StatusEnum status;

    public AbstractTask() {
        status = StatusEnum.prepared;
        time = System.currentTimeMillis();
    }

    public StatusEnum getStatus() {
        return status;
    }

    /**
     *
     * @return Time after setting "prepared" or " requested".
     */
    public long GetTimeMSec() {
        return System.currentTimeMillis() - time;
    }

    /**
     * Sets the status to "requested", fixes the time.
     */
    public void SetRequested() {
        status = StatusEnum.requested;
        time = System.currentTimeMillis();
    }

    /**
     * Check, set status if timeout expired.
     *
     * @param timeout Timeout in seconds
     * @return True if timeout expired
     */
    public boolean IsTimeout(int timeout) {
        if (System.currentTimeMillis() - time < timeout * 1000) {
            return false;
        }
        status = StatusEnum.exspired;
        return true;
    }
}
