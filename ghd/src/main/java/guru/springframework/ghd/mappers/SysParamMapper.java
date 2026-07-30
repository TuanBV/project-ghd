package guru.springframework.ghd.mappers;

import guru.springframework.ghd.dto.sysparam.SysParamResponse;
import guru.springframework.ghd.entities.SysParam;
import org.mapstruct.Mapper;

import java.util.*;

@Mapper(componentModel = "spring")
public interface SysParamMapper {
    SysParam sysParamResponseToSysParam(SysParamResponse sysParamResponse);

    SysParamResponse sysParamToSysParamResponse(SysParam sysParam);

    List<SysParamResponse> listSysParamToListSysParamResponse(List<SysParam> sysParam);

    List<SysParamResponse> toResponseList(List<SysParam> entities);
}
