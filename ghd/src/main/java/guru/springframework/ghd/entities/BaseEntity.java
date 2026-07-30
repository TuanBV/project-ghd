package guru.springframework.ghd.entities;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
public class BaseEntity {
    @Column(nullable = false, columnDefinition = "int default 0")
    @Builder.Default
    protected Integer delFlag = 0;
    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    protected LocalDateTime createdDate;
    @UpdateTimestamp
    @Column(name = "updated_date")
    protected LocalDateTime updatedDate;
}
