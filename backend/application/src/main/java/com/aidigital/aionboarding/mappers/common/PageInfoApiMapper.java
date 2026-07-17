package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.CountResponseV1;
import com.aidigital.aionboarding.api.v1.model.PageInfoV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

/**
 * Builds the shared {@link PageInfoV1} pagination contract from a Spring Data {@link Page}, and
 * the shared {@link CountResponseV1} count-only contract used by the Library count endpoints.
 */
@Mapper(config = ApplicationMapperConfig.class)
public interface PageInfoApiMapper {

	/**
	 * Builds pagination metadata from a Spring Data page result.
	 *
	 * @param page the page whose metadata should be exposed
	 * @return the equivalent {@link PageInfoV1} contract value
	 */
	default PageInfoV1 toPageInfoV1(Page<?> page) {
		PageInfoV1 info = new PageInfoV1();
		info.setPage(page.getNumber());
		info.setSize(page.getSize());
		info.setTotalElements(page.getTotalElements());
		info.setTotalPages(page.getTotalPages());
		info.setHasNext(page.hasNext());
		info.setHasPrevious(page.hasPrevious());
		return info;
	}

	/**
	 * Builds a count-only response body from a total-elements count.
	 *
	 * @param totalElements the total number of items matching the filter
	 * @return the equivalent {@link CountResponseV1} contract value
	 */
	default CountResponseV1 toCountResponseV1(long totalElements) {
		CountResponseV1 response = new CountResponseV1();
		response.setTotalElements(totalElements);
		return response;
	}
}
