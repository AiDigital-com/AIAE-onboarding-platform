package com.aidigital.aionboarding.mappers.common;

import com.aidigital.aionboarding.api.v1.model.CountResponseV1;
import com.aidigital.aionboarding.api.v1.model.PageInfoV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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
	@Mapping(target = "page", expression = "java(page.getNumber())")
	@Mapping(target = "size", expression = "java(page.getSize())")
	@Mapping(target = "totalElements", expression = "java(page.getTotalElements())")
	@Mapping(target = "totalPages", expression = "java(page.getTotalPages())")
	@Mapping(target = "hasNext", expression = "java(page.hasNext())")
	@Mapping(target = "hasPrevious", expression = "java(page.hasPrevious())")
	PageInfoV1 toPageInfoV1(Page<?> page);

	/**
	 * Builds a count-only response body from a total-elements count.
	 *
	 * @param totalElements the total number of items matching the filter
	 * @return the equivalent {@link CountResponseV1} contract value
	 */
	@Mapping(target = "totalElements", expression = "java(totalElements)")
	CountResponseV1 toCountResponseV1(Long totalElements);
}
