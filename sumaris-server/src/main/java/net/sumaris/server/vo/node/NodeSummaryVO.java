package net.sumaris.server.vo.node;

import lombok.Data;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
public class NodeSummaryVO {
    private String softwareName;
    private String softwareVersion;

    private String nodeLabel;
    private String nodeName;
}
