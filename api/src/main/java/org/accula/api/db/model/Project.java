package org.accula.api.db.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("projects")
@Data
@AllArgsConstructor
public class Project {

    @Id
    @Nullable
    private Long id;
    private String url;
    private String repositoryOwner;
    private String name;
    private String description;
    private String avatar;
    @Column("user_id")
    private Long creatorId;
    //private List<User> adminId;
    private Long openPullCount;

}
