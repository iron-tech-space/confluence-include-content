package com.irontechspace.plugins.macro;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.Renderer;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.confluence.xhtml.api.MacroDefinitionHandler;
import com.atlassian.confluence.xhtml.api.MacroDefinitionMarshallingStrategy;
import com.atlassian.confluence.xhtml.api.XhtmlContent;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Scanned
public class IncludeContent implements Macro {

    private static final Logger log = LoggerFactory.getLogger(IncludeContent.class);

    private PermissionManager permissionManager;
    private PageManager pageManager;
    private XhtmlContent xhtmlUtils;
    private Renderer renderer;

    private static final String SHARED_BLOCK = "shared-block";
    private static final String SHARED_BLOCK_KEY = "shared-block-key";

    @Autowired
    public IncludeContent(
            @ComponentImport PermissionManager permissionManager,
            @ComponentImport PageManager pageManager,
            @ComponentImport XhtmlContent xhtmlUtils,
            @ComponentImport Renderer renderer) {
        this.permissionManager = permissionManager;
        this.pageManager = pageManager;
        this.xhtmlUtils = xhtmlUtils;
        this.renderer = renderer;
    }


    public String execute(Map<String, String> map, String s, ConversionContext conversionContext) throws MacroExecutionException {

        String pageName = map.get("page");
        String macroName = map.get(SHARED_BLOCK_KEY);
        final List<MacroDefinition> macros = new ArrayList<MacroDefinition>();
        ContentEntityObject page = pageManager.getPage( conversionContext.getSpaceKey(), pageName );

        try{
            xhtmlUtils.handleMacroDefinitions(page.getBodyContent().getBody(), conversionContext, new MacroDefinitionHandler(){
                        @Override
                        public void handle(MacroDefinition macroDefinition){
                            macros.add(macroDefinition);
                        }
                    },
                    MacroDefinitionMarshallingStrategy.MARSHALL_MACRO);
        } catch (XhtmlException e) {
            throw new MacroExecutionException(e);
        }

        String outputHtml = null;

        if (!macros.isEmpty()) {
            for( MacroDefinition macroDefinition : macros ){
                if( macroDefinition.getName().equals(SHARED_BLOCK) && macroDefinition.getParameter(SHARED_BLOCK_KEY).equals(macroName)){
                    if(permissionManager.hasPermission(AuthenticatedUserThreadLocal.getUser(), Permission.VIEW, page)){
                        outputHtml = renderer.render(macroDefinition.getBodyText(), conversionContext);
                    } else {
                        outputHtml = "<div>You do not have permissions to view this content.</div>";
                    }
                }
            }
            if(outputHtml == null) {
                outputHtml = String.format("<div>Macro with name [%s] not found on page</div>", macroName);
            }
        } else {
            outputHtml = "<div>No macros found on the page</div>";
        }
        return outputHtml;
    }

    public BodyType getBodyType() { return BodyType.NONE; }

    public OutputType getOutputType() { return OutputType.BLOCK; }
}
