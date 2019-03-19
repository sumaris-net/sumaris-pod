package net.sumaris.importation.util.csv;

import net.sumaris.importation.service.vo.DataLoadResult;

import java.io.Closeable;
import java.io.IOException;

public interface FileReader extends Closeable {

    String[] readNext() throws IOException;

    String[] getHeaders();

    int getCurrentLine();

    DataLoadResult getResult();

    String getFileName();

}
