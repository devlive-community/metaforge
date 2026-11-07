package org.devlive.metaforge.service.repository;

import org.devlive.metaforge.executor.common.RunState;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.entity.WorkflowEntity;
import org.devlive.metaforge.service.persistence.Visibility;

import java.util.List;

public interface WorkflowRepository
        extends BaseRepository<WorkflowEntity, Long>
{
    List<WorkflowEntity> findAllByUserId(Long userId);

    List<WorkflowEntity> findAllByState(RunState state);

    @Override
    default Visibility visibleTo(UserEntity user)
    {
        return Visibility.where("userId", user.getId());
    }
}
