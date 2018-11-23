<%#
 Copyright 2013-2018 the original author or authors from the JHipster project.

 This file is part of the JHipster project, see http://www.jhipster.tech/
 for more information.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-%>
package <%=packageName%>.web.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import com.codahale.metrics.annotation.Timed;
<%_ if (dto !== 'mapstruct' || service === 'no') { _%>
import <%=packageName%>.domain.<%= entityClass %>;
<%_ } _%>
<%_ if (service !== 'no') { _%>
import <%=packageName%>.service.<%= entityClass %>Service;<% } else { %>
import <%=packageName%>.repository.<%= entityClass %>Repository;<% if (searchEngine === 'elasticsearch') { %>
import <%=packageName%>.repository.search.<%= entityClass %>SearchRepository;<% }} %>
import <%=packageName%>.web.rest.errors.BadRequestAlertException;
import <%=packageName%>.web.rest.util.HeaderUtil;<% if (pagination !== 'no') { %>
import <%=packageName%>.web.rest.util.PaginationUtil;<% } %>
<%_ if (dto === 'mapstruct') { _%>
import <%=packageName%>.service.dto.<%= entityClass %>DTO;
<%_ if (service === 'no') { _%>
import <%=packageName%>.service.mapper.<%= entityClass %>Mapper;
<%_ } } _%>
<%_ if (jpaMetamodelFiltering) {  _%>
import <%=packageName%>.service.dto.<%= entityClass %>Criteria;
import <%=packageName%>.service.<%= entityClass %>QueryService;
<%_ } _%>
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import springfox.documentation.annotations.ApiIgnore;
<%_ if (pagination !== 'no') { _%>
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
<%_ } _%>
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
<% if (validation) { %>
import javax.validation.Valid;<% } %>
import java.net.URI;
import java.net.URISyntaxException;
<%_ const viaService = service !== 'no';
    if (pagination === 'no' && dto === 'mapstruct' && !viaService && fieldsContainNoOwnerOneToOne === true) { _%>
import java.util.LinkedList;<% } %>
import java.util.List;
import java.util.Optional;<% if (databaseType === 'cassandra') { %>
import java.util.UUID;<% } %><% if (!viaService && (searchEngine === 'elasticsearch' || fieldsContainNoOwnerOneToOne === true)) { %>
import java.util.stream.Collectors;<% } %><% if (searchEngine === 'elasticsearch' || fieldsContainNoOwnerOneToOne === true) { %>
import java.util.stream.StreamSupport;<% } %><% if (searchEngine === 'elasticsearch') { %>

import static org.elasticsearch.index.query.QueryBuilders.*;<% } %>

/**
 * REST controller for managing <%= entityClass %>.
 */
@Api(description = "<%- formatAsApiDescription(removeInlineAnnotations(javadoc, entityClass)) %>")
@RestController
<%_ let mapping = '/api';
    let ignoreApi = null;
    let batch = [];

    let lastModified = null;
    let lastModifiedReturn = null;
    let lastModifiedArgs = [];

    if (typeof javadoc != 'undefined') {
        mapping = extractInlineAnnotationValueFromJavadoc(javadoc, 'request-mapping', '/api', '/api');
        ignoreApi = extractInlineAnnotationValueFromJavadoc(javadoc, 'api-ignore', '', '').split(/\s*,\s*/);
        batch = extractInlineAnnotationValueFromJavadoc(javadoc, 'api-batch', '', '').split(/\s*,\s*/);

        lastModified = extractInlineAnnotationValueFromJavadoc(javadoc, 'last-modified', null, '');
        if (lastModified != null){
            for (idx in fields) {
                let required = false;

                // 只取第一个 timestamp
                if (lastModifiedReturn == null && extractInlineAnnotationValueFromJavadoc(fields[idx].javadoc, 'timestamp') != null) {
                    lastModifiedReturn = fields[idx];
                }
                if (lastModified !== "serial" && extractInlineAnnotationValueFromJavadoc(fields[idx].javadoc, 'timestamp-key') != null) {
                    lastModifiedArgs.push(fields[idx]);
                }
            }
            if (lastModifiedReturn == null) {
                debug("field with pg-timestamp not found! ignore pg-last-modified flag.");
                lastModified = null;
            }
        }

    }
_%>
@RequestMapping("<%= mapping %>")
<%_ if (ignoreApi != null && ignoreApi.length == 0) { _%>
@ApiIgnore
<%_ } _%>
public class <%= entityClass %>Resource {

    private final Logger log = LoggerFactory.getLogger(<%= entityClass %>Resource.class);

    private static final String ENTITY_NAME = "<%= entityInstance %>";
    <%_
    const instanceType = (dto === 'mapstruct') ? entityClass + 'DTO' : entityClass;
    const instanceName = (dto === 'mapstruct') ? entityInstance + 'DTO' : entityInstance;
    _%><%- include('../../common/inject_template', {viaService: viaService, constructorName: entityClass + 'Resource', queryService: jpaMetamodelFiltering}); -%>

    /**
     * POST  /<%= entityApiUrl %> : Create a new <%= entityInstance %>.
     *
     * @param <%= instanceName %> the <%= instanceName %> to create
     * @return the ResponseEntity with status 201 (Created) and with body the new <%= instanceName %>, or with status 400 (Bad Request) if the <%= entityInstance %> has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    <%_ if (ignoreApi != null && ignoreApi.indexOf('create') != -1) { _%>
    @ApiIgnore
    <%_ } _%>
    @PostMapping("/<%= entityApiUrl %>")
    @Timed
    public ResponseEntity<<%= instanceType %>> create<%= entityClass %>(<% if (validation) { %>@Valid <% } %>@RequestBody <%= instanceType %> <%= instanceName %>) throws URISyntaxException {
        log.debug("REST request to save <%= entityClass %> : {}", <%= instanceName %>);
        <%= instanceType %> result = _create<%= entityClass %>(<%= instanceName %>);
        return ResponseEntity.created(new URI("<%= mapping %>/<%= entityApiUrl %>/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    <%= instanceType %> _create<%= entityClass %>(<%= instanceType %> <%= instanceName %>) throws URISyntaxException {
        if (<%= instanceName %>.getId() != null) {
            throw new BadRequestAlertException("A new <%= entityInstance %> cannot already have an ID", ENTITY_NAME, "idexists");
        }<%- include('../../common/save_template', {viaService: viaService, returnDirectly: false}); -%>
        return result;
    }

    <%_ if (batch.indexOf('create') != -1) { _%>
    /**
     * POST  /<%= entityApiUrl %>-batch : 批量插入 <%= entityInstance %>.
     *
     * @param <%= instanceName %>List the <%= instanceName %> list to create
     * @return the ResponseEntity with status 201 (Created) and with body the new ID, or with status 400 (Bad Request) if the <%= entityInstance %> has already an ID
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    <%_ if (ignoreApi != null && ignoreApi.indexOf('batch-create') != -1) { _%>
    @ApiIgnore
    <%_ } _%>
    @PostMapping("/<%= entityApiUrl %>/batch")
    @Timed
    public ResponseEntity<List<<%= pkType %>>> create<%= entityClass %>Batch(<% if (validation) { %>@Valid <% } %>@RequestBody List<<%= instanceType %>> <%= instanceName %>List) throws URISyntaxException {
        log.debug("REST request to batch save <%= entityClass %> : {}", <%= instanceName %>List);
        List<<%= pkType %>> results = new java.util.ArrayList<>(<%= instanceName %>List.size());
        for(<%= instanceType %> <%= instanceName %> : <%= instanceName %>List) {
            <%= instanceType %> result = _create<%= entityClass %>(<%= instanceName %>);
            results.add(result.getId());
        }
        return ResponseEntity.ok().body(results);
    }
    <%_ } _%>

    /**
     * PUT  /<%= entityApiUrl %> : Updates an existing <%= entityInstance %>.
     *
     * @param <%= instanceName %> the <%= instanceName %> to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated <%= instanceName %>,
     * or with status 400 (Bad Request) if the <%= instanceName %> is not valid,
     * or with status 500 (Internal Server Error) if the <%= instanceName %> couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    <%_ if (ignoreApi != null && ignoreApi.indexOf('update') != -1) { _%>
    @ApiIgnore
    <%_ } _%>
    @PutMapping("/<%= entityApiUrl %>")
    @Timed
    public ResponseEntity<<%= instanceType %>> update<%= entityClass %>(<% if (validation) { %>@Valid <% } %>@RequestBody <%= instanceType %> <%= instanceName %>) throws URISyntaxException {
        log.debug("REST request to update <%= entityClass %> : {}", <%= instanceName %>);
        <%= instanceType %> result = _update<%= entityClass %>(<%= instanceName %>);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(ENTITY_NAME, <%= instanceName %>.getId().toString()))
            .body(result);
    }

    <%= instanceType %> _update<%= entityClass %>(<%= instanceType %> <%= instanceName %>) throws URISyntaxException {
        if (<%= instanceName %>.getId() == null) {
            return _create<%= entityClass %>(<%= instanceName %>);
        }<%- include('../../common/save_template', {viaService: viaService, returnDirectly: false}); -%>
        return result;
    }

    <%_ if (batch.indexOf('update') != -1) { _%>
    /**
     * PUT  /<%= entityApiUrl %>-batch : Batch updates an existing <%= entityInstance %>.
     *
     * @param <%= instanceName %>List the <%= instanceName %>List to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated <%= instanceName %>,
     * or with status 400 (Bad Request) if the <%= instanceName %> is not valid,
     * or with status 500 (Internal Server Error) if the <%= instanceName %> couldn't be updated
     * @throws URISyntaxException if the Location URI syntax is incorrect
     */
    <%_ if (ignoreApi != null && ignoreApi.indexOf('batch-update') != -1) { _%>
    @ApiIgnore
    <%_ } _%>
    @PutMapping("/<%= entityApiUrl %>/batch")
    @Timed
    public ResponseEntity<List<<%= pkType %>>> update<%= entityClass %>Batch(<% if (validation) { %>@Valid <% } %>@RequestBody List<<%= instanceType %>> <%= instanceName %>List) throws URISyntaxException {
        log.debug("REST request to update <%= entityClass %> : {}", <%= instanceName %>List);
        List<<%= pkType %>> results = new java.util.ArrayList<>(<%= instanceName %>List.size());
        for(<%= instanceType %> <%= instanceName %> : <%= instanceName %>List) {
            <%= instanceType %> result = _update<%= entityClass %>(<%= instanceName %>);
            results.add(result.getId());
        }
        return ResponseEntity.ok().body(results);
    }
    <%_ } _%>

    /**
     * GET  /<%= entityApiUrl %> : get all the <%= entityInstancePlural %>.
     *<% if (pagination !== 'no') { %>
     * @param pageable the pagination information<% } if (jpaMetamodelFiltering) { %>
     * @param criteria the criterias which the requested entities should match<% } else if (fieldsContainNoOwnerOneToOne) { %>
     * @param filter the filter of the request<% } %>
     * @return the ResponseEntity with status 200 (OK) and the list of <%= entityInstancePlural %> in body
     */
    <%_ if (ignoreApi != null && ignoreApi.indexOf('getall') != -1) { _%>
    @ApiIgnore
    <%_ } _%>
    @GetMapping("/<%= entityApiUrl %>")
    @Timed<%- include('../../common/get_all_template', {viaService: viaService}); -%>

    /**
     * GET  /<%= entityApiUrl %>/:id : get the "id" <%= entityInstance %>.
     *
     * @param id the id of the <%= instanceName %> to retrieve
     * @return the ResponseEntity with status 200 (OK) and with body the <%= instanceName %>, or with status 404 (Not Found)
     */
    <%_ if (ignoreApi != null && ignoreApi.indexOf('get') != -1) { _%>
    @ApiIgnore
    <%_ } _%>
    @GetMapping("/<%= entityApiUrl %>/{id}")
    @Timed
    public ResponseEntity<<%= instanceType %>> get<%= entityClass %>(@PathVariable <%= pkType %> id) {
        log.debug("REST request to get <%= entityClass %> : {}", id);<%- include('../../common/get_template', {viaService: viaService, returnDirectly:false}); -%>
        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(<%= instanceName %>));
    }

    /**
     * DELETE  /<%= entityApiUrl %>/:id : delete the "id" <%= entityInstance %>.
     *
     * @param id the id of the <%= instanceName %> to delete
     * @return the ResponseEntity with status 200 (OK)
     */
    <%_ if (ignoreApi != null && ignoreApi.indexOf('delete') != -1) { _%>
    @ApiIgnore
    <%_ } _%>
    @DeleteMapping("/<%= entityApiUrl %>/{id}")
    @Timed
    public ResponseEntity<Void> delete<%= entityClass %>(@PathVariable <%= pkType %> id) {
        log.debug("REST request to delete <%= entityClass %> : {}", id);<%- include('../../common/delete_template', {viaService: viaService}); -%>
        return ResponseEntity.ok().headers(HeaderUtil.createEntityDeletionAlert(ENTITY_NAME, id<% if (pkType !== 'String') { %>.toString()<% } %>)).build();
    }

    <%_ if (lastModified != null) {
        let comma = '';
        let mcomma = '';
        let args = '';
        let callArgs = '';
        let brackets = '';
        let method = 'findTop' + lastModifiedReturn.fieldInJavaBeanMethod + 'By';
        for (idx in lastModifiedArgs) {
            args = args + comma + lastModifiedArgs[idx].fieldType + ' ' + lastModifiedArgs[idx].fieldName;
            callArgs = callArgs + comma + lastModifiedArgs[idx].fieldName;
            method = method + mcomma + lastModifiedArgs[idx].fieldInJavaBeanMethod;
            brackets = brackets + comma + '{}';
            comma = ', ';
            mcomma = 'And';
        }
        method = method + 'OrderBy' + lastModifiedReturn.fieldInJavaBeanMethod + 'Desc';
        let repoOrService = entityInstance + (viaService? 'Service': 'Repository');
    _%>
    /**
     * 获取对象的最后修改时间
     */
    <%_ if (ignoreApi != null && ignoreApi.indexOf('last-modified') != -1) { _%>
    @ApiIgnore
    <%_ } _%>
    @GetMapping("/<%= entityApiUrl %>/last-modified")
    @Timed
    public ResponseEntity<<%= lastModifiedReturn.fieldType %>> getLastModified<%= entityClass %>(<%= args %>) {
        log.debug("REST request to get last-modified <%= entityClass %> : <%= brackets %>"<%= brackets.length == 0? '': ',' %> <%= callArgs %>);
        <%= lastModifiedReturn.fieldType %> ret = <%= repoOrService %>.<%= method %>(<%= callArgs %>);
        return ResponseUtil.wrapOrNotFound(Optional.ofNullable(ret));
    }
    <%_ } _%>

    <% if (searchEngine === 'elasticsearch') { %>
    /**
     * SEARCH  /_search/<%= entityApiUrl %>?query=:query : search for the <%= entityInstance %> corresponding
     * to the query.
     *
     * @param query the query of the <%= entityInstance %> search<% if (pagination !== 'no') { %>
     * @param pageable the pagination information<% } %>
     * @return the result of the search
     */
    <%_ if (ignoreApi != null && ignoreApi.indexOf('search') != -1) { _%>
    @ApiIgnore
    <%_ } _%>
    @GetMapping("/_search/<%= entityApiUrl %>")
    @Timed<%- include('../../common/search_template', {viaService: viaService}); -%><% } %>
}
