/*
 ***************************************************************************************
 *  Copyright (C) 2006 EsperTech, Inc. All rights reserved.                            *
 *  http://www.espertech.com/esper                                                     *
 *  http://www.espertech.com                                                           *
 *  ---------------------------------------------------------------------------------- *
 *  The software in this package is published under the terms of the GPL license       *
 *  a copy of which has been included with this distribution in the license.txt file.  *
 ***************************************************************************************
 */
package com.espertech.esper.epl.core.select.eval;

import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.EventType;
import com.espertech.esper.codegen.base.CodegenBlock;
import com.espertech.esper.codegen.base.CodegenClassScope;
import com.espertech.esper.codegen.base.CodegenMember;
import com.espertech.esper.codegen.base.CodegenMethodScope;
import com.espertech.esper.epl.expression.codegen.CodegenLegoMayVoid;
import com.espertech.esper.codegen.model.expression.CodegenExpression;
import com.espertech.esper.epl.core.engineimport.EngineImportService;
import com.espertech.esper.epl.core.select.SelectExprProcessor;
import com.espertech.esper.epl.core.select.SelectExprProcessorForge;
import com.espertech.esper.epl.expression.codegen.ExprForgeCodegenSymbol;
import com.espertech.esper.codegen.base.CodegenMethodNode;
import com.espertech.esper.epl.core.select.SelectExprProcessorCodegenSymbol;
import com.espertech.esper.epl.expression.core.ExprEvaluator;
import com.espertech.esper.epl.expression.core.ExprEvaluatorContext;
import com.espertech.esper.epl.util.ExprNodeUtilityRich;
import com.espertech.esper.event.EventAdapterService;

import static com.espertech.esper.codegen.model.expression.CodegenExpressionBuilder.*;

public class EvalSelectNoWildcardObjectArray implements SelectExprProcessor, SelectExprProcessorForge {

    private final SelectExprForgeContext context;
    private final EventType resultEventType;
    private ExprEvaluator[] evaluators;

    public EvalSelectNoWildcardObjectArray(SelectExprForgeContext context, EventType resultEventType) {
        this.context = context;
        this.resultEventType = resultEventType;
    }

    public SelectExprProcessor getSelectExprProcessor(EngineImportService engineImportService, boolean isFireAndForget, String statementName) {
        if (evaluators == null) {
            evaluators = ExprNodeUtilityRich.getEvaluatorsMayCompile(context.getExprForges(), engineImportService, this.getClass(), isFireAndForget, statementName);
        }
        return this;
    }

    public EventBean process(EventBean[] eventsPerStream, boolean isNewData, boolean isSynthesize, ExprEvaluatorContext exprEvaluatorContext) {
        EventAdapterService eventAdapterService = context.getEventAdapterService();

        // Evaluate all expressions and build a map of name-value pairs
        Object[] props = new Object[evaluators.length];
        for (int i = 0; i < evaluators.length; i++) {
            Object evalResult = evaluators[i].evaluate(eventsPerStream, isNewData, exprEvaluatorContext);
            props[i] = evalResult;
        }

        return eventAdapterService.adapterForTypedObjectArray(props, resultEventType);
    }

    public CodegenMethodNode processCodegen(CodegenMember memberResultEventType, CodegenMember memberEventAdapterService, CodegenMethodScope codegenMethodScope, SelectExprProcessorCodegenSymbol selectSymbol, ExprForgeCodegenSymbol exprSymbol, CodegenClassScope codegenClassScope) {
        CodegenMethodNode methodNode = codegenMethodScope.makeChild(EventBean.class, this.getClass(), codegenClassScope);
        CodegenBlock block = methodNode.getBlock()
                .declareVar(Object[].class, "props", newArrayByLength(Object.class, constant(this.context.getExprForges().length)));
        for (int i = 0; i < this.context.getExprForges().length; i++) {
            CodegenExpression expression = CodegenLegoMayVoid.expressionMayVoid(Object.class, this.context.getExprForges()[i], methodNode, exprSymbol, codegenClassScope);
            block.assignArrayElement("props", constant(i), expression);
        }
        block.methodReturn(exprDotMethod(member(memberEventAdapterService.getMemberId()), "adapterForTypedObjectArray", ref("props"), member(memberResultEventType.getMemberId())));
        return methodNode;
    }

    public EventType getResultEventType() {
        return resultEventType;
    }
}
