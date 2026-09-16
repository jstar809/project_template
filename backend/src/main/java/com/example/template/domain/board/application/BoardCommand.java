package com.example.template.domain.board.application;

import com.example.template.domain.board.domain.Permission;

/** 게시판 생성/수정 입력. */
public record BoardCommand(
        String slug,
        String name,
        String description,
        Permission readPermission,
        Permission writePermission,
        boolean commentEnabled,
        boolean attachmentEnabled,
        boolean active) {}
