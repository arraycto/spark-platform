package com.spark.platform.flowable.biz.service.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.spark.platform.common.base.constants.ProcessConstants;
import com.spark.platform.common.utils.BeanCopyUtil;
import com.spark.platform.flowable.api.DTO.DeploymentDTO;
import com.spark.platform.flowable.api.DTO.ProcessDefinitionDTO;
import com.spark.platform.flowable.api.vo.DeploymentVO;
import com.spark.platform.flowable.api.vo.ProcessDefinitionVO;
import com.spark.platform.flowable.biz.service.ActProcessService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.*;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.ui.modeler.domain.Model;
import org.flowable.ui.modeler.serviceapi.ModelService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipInputStream;

/**
 * @author: wangdingfeng
 * @Date: 2020/4/4 21:49
 * @Description: 流程对象Service
 */
@Service
@Slf4j
public class ActProcessServiceImpl implements ActProcessService {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private ModelService modelService;

    @Override
    public DeploymentBuilder createDeployment() {
        return repositoryService.createDeployment();
    }

    @Override
    public DeploymentQuery createDeploymentQuery() {
        return repositoryService.createDeploymentQuery();
    }

    @Override
    public ProcessDefinitionQuery createProcessDefinitionQuery() {
        return repositoryService.createProcessDefinitionQuery();
    }

    @Override
    public Deployment deploy(String bpmnFileUrl) {
        Deployment deploy = createDeployment().addClasspathResource(bpmnFileUrl).deploy();
        log.info("部署成功，当前部署ID:{}-部署的key{}-部署name;{}",deploy.getId(),deploy.getKey(),deploy.getName());
        return deploy;
    }

    @Override
    public DeploymentVO deployByModel(String modelId, String category) {
        Model model = modelService.getModel(modelId);
        byte[] bpmnBytes = modelService.getBpmnXML(model);
        String processName = model.getName();
        if (!StrUtil.endWith(processName, ".bpmn20.xml")){
            processName += ".bpmn20.xml";
        }
        DeploymentVO deploymentVO = new DeploymentVO();
        Deployment deployment = repositoryService.createDeployment()
                .addBytes(processName, bpmnBytes)
                .name(model.getName())
                .key(model.getKey())
                .deploy();
        //忽略二进制文件（模板文件、模板图片）返回
        BeanUtils.copyProperties(deployment,deploymentVO,"resources");
        return deploymentVO;
    }

    @Override
    public Deployment deploy(String url, String pngUrl) {
        Deployment deploy = createDeployment().addClasspathResource(url).addClasspathResource(pngUrl).deploy();
        log.info("部署成功，当前部署ID:{}-部署的key{}-部署name;{}",deploy.getId(),deploy.getKey(),deploy.getName());
        return deploy;
    }

    @Override
    public DeploymentVO deploy(String name, String tenantId, String category, ZipInputStream zipInputStream) {
        Deployment deployment = createDeployment().addZipInputStream(zipInputStream)
                .name(name).category(category).tenantId(tenantId).deploy();
        DeploymentVO deploymentVO = new DeploymentVO();
        //忽略二进制文件（模板文件、模板图片）返回
        BeanUtils.copyProperties(deployment,deploymentVO,"resources");
        return deploymentVO;
    }

    @Override
    public Deployment deployBpmnAndDrl(String url, String drlUrl) {
        Deployment deploy = createDeployment().addClasspathResource(url).addClasspathResource(drlUrl).deploy();
        log.info("部署成功，当前部署ID:{}-部署的key{}-部署name;{}",deploy.getId(),deploy.getKey(),deploy.getName());
        return deploy;
    }

    @Override
    public Deployment deploy(String url, String name, String category) {
        Deployment deploy = createDeployment().addClasspathResource(url).name(name).category(category).deploy();
        log.info("部署成功，当前部署ID:{}-部署的key{}-部署name;{}",deploy.getId(),deploy.getKey(),deploy.getName());
        return deploy;
    }

    @Override
    public Deployment deploy(String url, String pngUrl, String name, String category) {
        Deployment deploy = createDeployment().addClasspathResource(url).addClasspathResource(pngUrl)
                .name(name).category(category).deploy();
        log.info("部署成功，当前部署ID:{}-部署的key{}-部署name;{}",deploy.getId(),deploy.getKey(),deploy.getName());
        return deploy;
    }

    @Override
    public boolean exist(String processDefinitionKey) {
        ProcessDefinitionQuery processDefinitionQuery
                = createProcessDefinitionQuery().processDefinitionKey(processDefinitionKey);
        long count = processDefinitionQuery.count();
        return count > 0 ? true : false;
    }

    @Override
    public DeploymentVO deploy(String name, String tenantId, String category, InputStream in) {
        Deployment deployment = createDeployment().addInputStream(name + ProcessConstants.BPMN_FILE_SUFFIX, in)
                .name(name)
                .tenantId(tenantId)
                .category(category)
                .deploy();
        DeploymentVO deploymentVO = new DeploymentVO();
        //忽略二进制文件（模板文件、模板图片）返回
        BeanUtils.copyProperties(deployment,deploymentVO,"resources");
        return deploymentVO;

    }

    @Override
    public ProcessDefinition queryByProcessDefinitionKey(String processDefinitionKey) {
        ProcessDefinition processDefinition
                = createProcessDefinitionQuery()
                .processDefinitionKey(processDefinitionKey)
                .active().singleResult();
        return processDefinition;
    }

    @Override
    public Page<ProcessDefinitionVO> listDefinitionPage(ProcessDefinitionDTO processDefinitionDTO, Page page) {
        int firstResult = (int)((page.getCurrent()-1)*page.getSize());
        int maxResults = (int)(page.getCurrent()*page.getSize());
        ProcessDefinitionQuery processDefinitionQuery = createProcessDefinitionQuery();
        if(StringUtils.isNotBlank(processDefinitionDTO.getName())){
            processDefinitionQuery.processDefinitionNameLike(processDefinitionDTO.getName());
        }
        if(StringUtils.isNotBlank(processDefinitionDTO.getKey())){
            processDefinitionQuery.processDefinitionKeyLike(processDefinitionDTO.getKey());
        }
        if(StringUtils.isNotBlank(processDefinitionDTO.getCategory())){
            processDefinitionQuery.processDefinitionCategoryLike(processDefinitionDTO.getCategory());
        }
        long count = processDefinitionQuery.count();
        List<ProcessDefinition> processDefinitionList = processDefinitionQuery.orderByDeploymentId().desc().listPage(firstResult,maxResults);
        List<ProcessDefinitionVO> processDefinitionVOS = BeanCopyUtil.copyListProperties(processDefinitionList,ProcessDefinitionVO::new);
        page.setRecords(processDefinitionVOS);
        page.setTotal(count);
        return page;
    }

    @Override
    public Deployment deployName(String deploymentName) {
        List<Deployment> list = repositoryService
                .createDeploymentQuery()
                .deploymentName(deploymentName).list();
        Assert.notNull(list, "list must not be null");
        return list.get(0);
    }

    @Override
    public void addCandidateStarterUser(String processDefinitionKey, String userId) {
        repositoryService.addCandidateStarterUser(processDefinitionKey, userId);
    }

    @Override
    public void deleteDeployment(String deploymentId, boolean cascade) {
        repositoryService.deleteDeployment(deploymentId,cascade);
        log.info("删除部署流程成功，部署id:{}",deploymentId);
    }

    @Override
    public Page<DeploymentVO> listPage(DeploymentDTO deploymentDTO, Page page) {
        int firstResult = (int)((page.getCurrent()-1)*page.getSize());
        int maxResults = (int)(page.getCurrent()*page.getSize());
        DeploymentQuery deploymentQuery = createDeploymentQuery();
        if(StringUtils.isNotBlank(deploymentDTO.getDeploymentName())){
            deploymentQuery.deploymentNameLike(deploymentDTO.getDeploymentName());
        }
        if(StringUtils.isNotBlank(deploymentDTO.getDeploymentKey())){
            deploymentQuery.deploymentKeyLike(deploymentDTO.getDeploymentKey());
        }
        if(StringUtils.isNotBlank(deploymentDTO.getCategory())){
            deploymentQuery.deploymentCategoryLike(deploymentDTO.getCategory());
        }
        long count = deploymentQuery.count();
        List<Deployment> deployments = deploymentQuery.orderByDeploymenTime().desc().listPage(firstResult,maxResults);
        List<DeploymentVO> deploymentVOS = BeanCopyUtil.copyListProperties(deployments,DeploymentVO::new,"resources");
        page.setRecords(deploymentVOS);
        page.setTotal(count);
        return page;
    }

    @Override
    public InputStream resourceRead(String procDefId, String proInsId, String resType) {
        if (StringUtils.isBlank(procDefId)){
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(proInsId).singleResult();
            procDefId = processInstance.getProcessDefinitionId();
        }
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(procDefId).singleResult();
        String resourceName = "";
        if (resType.equals("image")) {
            resourceName = processDefinition.getDiagramResourceName();
        } else if (resType.equals("xml")) {
            resourceName = processDefinition.getResourceName();
        }
        InputStream resourceAsStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), resourceName);
        return resourceAsStream;
    }

    @Override
    public void downloadXml(HttpServletResponse response, HttpServletRequest request, String processDefinitionId) {
        try {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
            InputStream inputStream = repositoryService.getResourceAsStream(processDefinition.getDeploymentId(), processDefinition.getResourceName());
            byte[] data = IoUtil.readBytes(inputStream);
            response.reset();
            String fileName = processDefinition.getResourceName();
            //浏览器设置
            String userAgent = request.getHeader("User-Agent");
            if (userAgent.contains("MSIE") || userAgent.contains("Trident")) {
                //IE浏览器处理
                fileName = java.net.URLEncoder.encode(fileName, "UTF-8");
            } else {
                // 非IE浏览器的处理：
                fileName = new String(fileName.getBytes("UTF-8"), "ISO-8859-1");
            }
            //下载的文件携带这个名称
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
            response.addHeader("Content-Length", "" + data.length);
            response.setContentType("application/octet-stream; charset=UTF-8");
            IOUtils.write(data, response.getOutputStream());
        }catch (Exception e){
            log.error("获取流异常!",e);
        }
    }
}
