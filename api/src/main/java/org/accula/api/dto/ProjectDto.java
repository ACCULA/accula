package org.accula.api.dto;

import lombok.Setter;
import org.accula.api.handlers.response.ResponseBody;

@Setter
public class ProjectDto implements ResponseBody {

    private String url;
    private String repositoryOwner;
    private String name;
    private String description;
    private String avatar;
    private Long creatorId;
    private int openPullCount;
}
