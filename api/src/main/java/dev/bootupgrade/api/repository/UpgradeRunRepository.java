package dev.bootupgrade.api.repository;

import dev.bootupgrade.api.model.UpgradeRunEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UpgradeRunRepository extends JpaRepository<UpgradeRunEntity, String> {

    List<UpgradeRunEntity> findAllByOrderByCreatedAtDesc();
}
