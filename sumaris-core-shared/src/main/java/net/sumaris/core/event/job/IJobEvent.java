package net.sumaris.core.event.job;


import java.io.Serializable;

public interface IJobEvent<V extends Serializable> {

    interface Fields {
        String OPERATION = "operation";
        String ID = "id";
    }

    enum JobEventOperation {
        START,
        PROGRESS,
        END
    }

    JobEventOperation getOperation();

    int getId();

    void setId(int id);

    V getData();

    void setData(V data);
}
