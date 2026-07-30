package guru.springframework.ghd.entities;

import guru.springframework.ghd.config.JsonStringListConverter;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Table(name = "policy")
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Policy extends BaseEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, columnDefinition = "varchar(36)", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String packageName;

    @Convert(converter = JsonStringListConverter.class)
    @Column(columnDefinition = "json")
    private List<String> policies = new ArrayList<>();

    @Convert(converter = JsonStringListConverter.class)
    @Column(columnDefinition = "json")
    private List<String> afterSales = new ArrayList<>();

    @Convert(converter = JsonStringListConverter.class)
    @Column(columnDefinition = "json")
    private List<String> gifts = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "int default 0")
    @Builder.Default
    private Integer isActive = 0;
}