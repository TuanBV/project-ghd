package guru.springframework.ghd.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "sys_param", indexes = {@Index(name = "idx_param_key", columnList = "param_key")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SysParam extends BaseEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Integer version;

    @Column(nullable = false, unique = true)
    private String paramKey;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String paramValue;

    @Column(nullable = false)
    private String paramName;

    private String groupCode;

    private String description;
}