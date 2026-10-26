package org.devlive.metaforge.service.service;

import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.body.UploadBody;
import org.devlive.metaforge.service.body.UserNameBody;
import org.devlive.metaforge.service.body.UserPasswordBody;
import org.devlive.metaforge.service.entity.PageEntity;
import org.devlive.metaforge.service.entity.UserEntity;
import org.devlive.metaforge.service.entity.convert.AvatarEntity;
import org.devlive.metaforge.service.entity.itransient.user.UserEditorEntity;
import org.devlive.metaforge.service.model.AiModel;
import org.devlive.metaforge.service.record.TreeRecord;

import java.util.List;

public interface UserService
        extends BaseService<UserEntity>
{
    CommonResponse<UserEntity> saveOrUpdate(UserEntity configure);

    CommonResponse<Object> authenticate(UserEntity configure);

    CommonResponse<UserEntity> info(String code);

    CommonResponse<Long> changePassword(UserPasswordBody configure);

    CommonResponse<Long> changeUsername(UserNameBody configure);

    /**
     * Check whether the given username can be used for renaming:
     * format valid (4-20 chars, letters/numbers/underscores, not starting
     * with a digit) and not taken by another account.
     */
    CommonResponse<Boolean> checkUsername(String username);

    /**
     * Test the connectivity of the AI assistant service:
     * performs a GET request against {host}/models with the given token.
     */
    CommonResponse<Boolean> testChat(AiModel configure);

    CommonResponse<Long> changeThirdConfigure(AiModel configure);

    CommonResponse<List<Object>> getSugs(Long id);

    CommonResponse<List<TreeRecord>> getMenus();

    CommonResponse<PageEntity<UserEntity>> getAll(FilterBody filter);

    CommonResponse<UserEntity> changeEditorConfigure(UserEditorEntity configure);

    CommonResponse<AvatarEntity> uploadAvatar(UploadBody configure);

    CommonResponse<UserEntity> changeNotify(UserEntity configure);
}
