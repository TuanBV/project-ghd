package guru.springframework.ghd.utils;

import guru.springframework.ghd.constants.DefaultPage;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

public class PaginationUtil {
    public static PageRequest getPageRequest(Integer pageNumber, Integer pageSize, String sortField, String sortDir) {
        int queryPageNumber = (pageNumber != null && pageNumber > DefaultPage.PAGE)
                ? pageNumber - 1 : DefaultPage.PAGE;

        int queryPageSize = (pageSize != null && pageSize > 0)
                ? pageSize : DefaultPage.SIZE_CLIENT;

        String field = StringUtils.hasText(sortField) ? sortField : DefaultPage.ID;

        Sort sort = DefaultPage.ID.equalsIgnoreCase(sortDir)
                ? Sort.by(field).ascending()
                : Sort.by(field).descending();

        return PageRequest.of(queryPageNumber, queryPageSize, sort);
    }
}
