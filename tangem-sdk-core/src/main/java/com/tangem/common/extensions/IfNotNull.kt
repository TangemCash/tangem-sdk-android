package com.tangem.common.extensions

/**
 * Created by Anton Zhilenkov on 12/01/2022.
 */

typealias NothingCallback<R> = () -> R

inline fun <R, A> ifNotNull(a: A?, block: (A) -> R): R? = if (a != null) block(a) else null

inline fun <R, A, B> ifNotNull(a: A?, b: B?, block: (A, B) -> R): R? = if (a != null && b != null) block(a, b) else null

inline fun <R, A, B, C> ifNotNull(a: A?, b: B?, c: C?, block: (A, B, C) -> R): R? =
    if (a != null && b != null && c != null) block(a, b, c) else null

@Suppress("ComplexCondition")
inline fun <R, A, B, C, D> ifNotNull(a: A?, b: B?, c: C?, d: D?, block: (A, B, C, D) -> R): R? =
    if (a != null && b != null && c != null && d != null) block(a, b, c, d) else null

inline fun <R, A> ifNotNullOr(a: A?, block: (A) -> R, orBloc: NothingCallback<R>): R =
    if (a != null) block(a) else orBloc()

inline fun <R, A, B> ifNotNullOr(a: A?, b: B?, block: (A, B) -> R, orBloc: NothingCallback<R>): R =
    if (a != null && b != null) block(a, b) else orBloc()

inline fun <R, A, B, C> ifNotNullOr(a: A?, b: B?, c: C?, block: (A, B, C) -> R, orBloc: NothingCallback<R>): R =
    if (a != null && b != null && c != null) block(a, b, c) else orBloc()

@Suppress("LongParameterList", "ComplexCondition")
inline fun <R, A, B, C, D> ifNotNullOr(
    a: A?,
    b: B?,
    c: C?,
    d: D?,
    block: (A, B, C, D) -> R,
    orBloc: NothingCallback<R>,
): R = if (a != null && b != null && c != null && d != null) block(a, b, c, d) else orBloc()
