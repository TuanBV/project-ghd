package guru.springframework.ghd.services;

import guru.springframework.ghd.dto.sysparam.SysParamRequest;
import guru.springframework.ghd.dto.sysparam.SysParamResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

public interface SysParamService {

    List<SysParamResponse> getList(String groupCode);

    List<SysParamResponse> getAll();

    Optional<SysParamResponse> getById(Long id);

    Optional<SysParamResponse> getByKey(String key);

    void updateSysParam(List<SysParamRequest> sysParamRequests);

    void uploadSitemap(MultipartFile file) throws IOException;

    void uploadSeoFile(String type, MultipartFile file) throws IOException;
}
