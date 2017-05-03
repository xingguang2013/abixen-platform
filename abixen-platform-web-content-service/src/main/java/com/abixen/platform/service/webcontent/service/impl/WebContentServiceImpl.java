/**
 * Copyright (c) 2010-present Abixen Systems. All rights reserved.
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.abixen.platform.service.webcontent.service.impl;

import com.abixen.platform.common.exception.PlatformRuntimeException;
import com.abixen.platform.service.webcontent.form.AdvancedWebContentForm;
import com.abixen.platform.service.webcontent.form.SearchWebContentForm;
import com.abixen.platform.service.webcontent.form.SimpleWebContentForm;
import com.abixen.platform.service.webcontent.form.WebContentForm;
import com.abixen.platform.service.webcontent.model.enumtype.WebContentType;
import com.abixen.platform.service.webcontent.model.impl.AdvancedWebContent;
import com.abixen.platform.service.webcontent.model.impl.SimpleWebContent;
import com.abixen.platform.service.webcontent.model.impl.Structure;
import com.abixen.platform.service.webcontent.model.impl.WebContent;
import com.abixen.platform.service.webcontent.repository.WebContentRepository;
import com.abixen.platform.service.webcontent.service.StructureService;
import com.abixen.platform.service.webcontent.service.WebContentService;
import com.abixen.platform.service.webcontent.util.AdvancedWebContentBuilder;
import com.abixen.platform.service.webcontent.util.SimpleWebContentBuilder;
import com.sun.org.apache.xerces.internal.parsers.DOMParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Transactional
@Service
public class WebContentServiceImpl implements WebContentService {

    private final WebContentRepository webContentRepository;
    private final StructureService structureService;

    @Autowired
    public WebContentServiceImpl(WebContentRepository webContentRepository,
                                 StructureService structureService) {
        this.webContentRepository = webContentRepository;
        this.structureService = structureService;
    }

    @Override
    public WebContent createWebContent(WebContentForm webContentForm) {
        log.debug("createWebContent() - webContentForm: {}", webContentForm);

        WebContent webContent = buildWebContent(webContentForm, null);

        return webContentRepository.save(webContent);
    }

    @Override
    public WebContent updateWebContent(WebContentForm advancedWebContentForm) {
        log.debug("updateAdvancedWebContent() - advancedWebContentForm: {}", advancedWebContentForm);
        WebContent webContentForUpdate = webContentRepository.findOne(advancedWebContentForm.getId());
        WebContent webContent = buildWebContent(advancedWebContentForm, webContentForUpdate);
        return webContentRepository.save(webContent);
    }

    @Override
    public WebContent findWebContent(Long id) {
        return webContentRepository.findOne(id);
    }

    private WebContent buildWebContent(WebContentForm webContentForm, WebContent webContent) {
        switch (webContentForm.getType()) {
            case SIMPLE:
                SimpleWebContentForm simpleWebContentForm = (SimpleWebContentForm) webContentForm;
                SimpleWebContentBuilder simpleWebContentBuilder;
                if (webContent == null) {
                    simpleWebContentBuilder = new SimpleWebContentBuilder();
                } else {
                    simpleWebContentBuilder = new SimpleWebContentBuilder((SimpleWebContent) webContent);
                }
                simpleWebContentBuilder.content(simpleWebContentForm.getContent());
                simpleWebContentBuilder.title(simpleWebContentForm.getTitle());
                webContent = simpleWebContentBuilder.build();
                break;

            case ADVANCED:
                AdvancedWebContentForm advancedWebContentForm = (AdvancedWebContentForm) webContentForm;
                AdvancedWebContentBuilder advancedWebContentBuilder;
                if (webContent == null) {
                    advancedWebContentBuilder = new AdvancedWebContentBuilder();
                } else {
                    advancedWebContentBuilder = new AdvancedWebContentBuilder((AdvancedWebContent) webContent);
                }
                advancedWebContentBuilder.content(advancedWebContentForm.getContent());
                advancedWebContentBuilder.title(advancedWebContentForm.getTitle());
                Structure structure = structureService.findStructureById(advancedWebContentForm.getStructure().getId());
                advancedWebContentBuilder.structure(structure);
                webContent = advancedWebContentBuilder.build();
                break;
            default:
        }
        return webContent;
    }

    @Override
    public Page<WebContent> getWebContents(Pageable pageable) {
        log.debug("getWebContents() - pageable: {}", pageable);
        return webContentRepository.findAll(pageable);
    }

    @Override
    public Page<WebContent> getWebContents(Pageable pageable, SearchWebContentForm searchWebContentForm) {
        log.debug("getWebContents() - pageable: {}", pageable);
        return webContentRepository.findAll(pageable);
    }

    @Override
    public WebContentForm getWebContent(Long id) {
        WebContent webContent = webContentRepository.getOne(id);
        if (webContent != null) {
            return getWebContentForm(webContent);
        } else {
            throw new PlatformRuntimeException("Content not found");
        }
    }

    @Override
    public void deleteWebContent(Long id) {
        log.debug("deleteWebContent() - id={}", id);
        webContentRepository.delete(id);
    }

    private WebContentForm getWebContentForm(WebContent webContent) {
        if (WebContentType.ADVANCED.equals(webContent.getType())) {
            return buildAdvanceWebContent(webContent);
        }
        return new WebContentForm(webContent);
    }

    private WebContentForm buildAdvanceWebContent(WebContent webContent) {
        //AdvancedWebContent advancedWebContentById = advancedWebContentService.findAdvancedWebContentById(webContent.getId());
        //String contentWithoutData = advancedWebContentById.getStructure().getTemplate().getContent();
        //String dataForContent = advancedWebContentById.getStructure().getContent();
        //getContentWithData(contentWithoutData, getParsedXml(dataForContent));
        return null;
    }

    private void getContentWithData(String contentWithoutData, Document parsedXml) {
        String contentToFill = contentWithoutData;
        for (String tag : getAllTag(contentWithoutData)) {
            contentWithoutData.replace(tag, getElementByTag(parsedXml, tag));
        }
    }

    private List<String> getAllTag(String contentWithoutData) {
        List<String> tags = new ArrayList<>();
        Matcher result = Pattern.compile("\\$\\{(.*?)}").matcher(contentWithoutData);
        while (result.find()) {
            tags.add(result.group());
        }
        return tags;
    }

    private String getElementByTag(Document parsedXml, String tag) {
        return parsedXml.getElementsByTagName(tag).item(0).getNodeValue();
    }

    private Document getParsedXml(String dataForContent) {
        DOMParser parser = new DOMParser();
        try {
            parser.parse(new InputSource(new java.io.StringReader(dataForContent)));
            return parser.getDocument();
        } catch (SAXException e) {
            throw new PlatformRuntimeException("Can't parse structure for advanced web content. Please check configuration.");
        } catch (IOException e) {
            throw new PlatformRuntimeException("Can't parse structure for advanced web content. Please check configuration.");
        }
    }
}