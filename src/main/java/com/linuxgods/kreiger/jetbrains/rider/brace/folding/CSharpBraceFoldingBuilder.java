package com.linuxgods.kreiger.jetbrains.rider.brace.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.FoldingGroup;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.jetbrains.rider.languages.fileTypes.csharp.psi.impl.CSharpParameterDeclaration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.jetbrains.rider.languages.fileTypes.csharp.kotoparser.lexer.CSharpTokenType.*;

public class CSharpBraceFoldingBuilder extends FoldingBuilderEx {
    private static final Logger log = Logger.getInstance(CSharpBraceFoldingBuilder.class);

    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull PsiElement psiElement, @NotNull Document document, boolean b) {
        FoldingGroup braceGroup = FoldingGroup.newGroup("brace");
        FoldingGroup spaceGroup = FoldingGroup.newGroup("space");
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        psiElement.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitWhiteSpace(@NotNull PsiWhiteSpace space) {
                ASTNode node = space.getNode();
                ASTNode prev = node.getTreePrev();
                if (prev != null && prev.getElementType() == WHITE_SPACE) return;
                ASTNode next = node.getTreeNext();
                if (next == null || next.getElementType() != WHITE_SPACE) return;
                ASTNode nextNext = next.getTreeNext();
                if (nextNext == null || nextNext.getElementType() != WHITE_SPACE) return;
                TextRange textRange = next.getTextRange();
                for (ASTNode n = nextNext.getTreeNext(); n != null; n = n.getTreeNext()) {
                    if (n.getElementType() != WHITE_SPACE) {
                        FoldingDescriptor foldingDescriptor = new FoldingDescriptor(next, textRange, spaceGroup, "");
                        descriptors.add(foldingDescriptor);
                        return;
                    }
                    textRange = textRange.union(n.getTreePrev().getTextRange());
                }
            }

            @Override
            public void visitElement(@NotNull PsiElement element) {
                checkAstNode(element.getNode());
                super.visitElement(element);
            }

            private void checkAstNode(ASTNode astNode) {
                IElementType type = astNode.getElementType();
                if (type != LBRACE && type != CATCH_KEYWORD && type != ELSE_KEYWORD && type != WHERE_KEYWORD) {
                    if (!(astNode.getPsi() instanceof CSharpParameterDeclaration)) {
                        return;
                    }
                }
                ASTNode treePrev = astNode.getTreePrev();
                if (treePrev == null) {
                    ASTNode treeParent = astNode.getTreeParent();
                    if (treeParent == null) return;
                    treePrev = treeParent.getTreePrev();
                }
                if (treePrev == null || treePrev.getElementType() != WHITE_SPACE) return;
                TextRange textRange = treePrev.getTextRange();
                ASTNode treePrevPrev = treePrev.getTreePrev();
                if (treePrevPrev != null && treePrevPrev.getElementType() == WHITE_SPACE) {
                    textRange = treePrevPrev.getTextRange().union(textRange);
                } else if (textRange.getLength() == 1 && treePrev.getText().charAt(0) != '\n') {
                    return;
                }
                FoldingDescriptor foldingDescriptor = new FoldingDescriptor(astNode, textRange, braceGroup, " ");
                descriptors.add(foldingDescriptor);
            }
        });

        return descriptors.isEmpty() ? FoldingDescriptor.EMPTY_ARRAY : descriptors.toArray(FoldingDescriptor[]::new);
    }

    @Override
    public @Nullable String getPlaceholderText(@NotNull ASTNode astNode) {
        return null;
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode astNode) {
        return true;
    }
}
