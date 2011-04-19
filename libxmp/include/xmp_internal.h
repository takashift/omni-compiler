/*
 * $TSUKUBA_Release: $
 * $TSUKUBA_Copyright:
 *  $
 */

#ifndef _XMP_INTERNAL
#define _XMP_INTERNAL

// --------------- including headers  --------------------------------
#include <stddef.h>
#include <stdbool.h>

// --------------- macro functions -----------------------------------
#ifdef DEBUG
#define _XMP_ASSERT(_flag) \
{ \
  if (!(_flag)) { \
    _XMP_unexpected_error(); \
  } \
}
#else
#define _XMP_ASSERT(_flag)
#endif

#define _XMP_RETURN_IF_SINGLE \
{ \
  if (_XMP_world_rank == 1) { \
    return; \
  } \
}

// --------------- structures ----------------------------------------
#define _XMP_comm void

// nodes descriptor
typedef struct _XMP_nodes_info_type {
  int size;

  // enable when is_member is true
  int rank;
  // -----------------------------
} _XMP_nodes_info_t;

typedef struct _XMP_nodes_type {
  _Bool is_member;
  int dim;
  int comm_size;

  // enable when is_member is true
  int comm_rank;
  _XMP_comm *comm;
  // -----------------------------

  _XMP_nodes_info_t info[1];
} _XMP_nodes_t;

// template desciptor
typedef struct _XMP_template_info_type {
  // enable when is_fixed is true
  long long ser_lower;
  long long ser_upper;
  unsigned long long ser_size;
  // ----------------------------
} _XMP_template_info_t;

typedef struct _XMP_template_chunk_type {
  // enable when is_owner is true
  long long par_lower;
  long long par_upper;
  // ----------------------------

  int par_stride;
  unsigned long long par_chunk_width;
  int dist_manner;
  _Bool is_regular_chunk;

  // enable when dist_manner is not _XMP_N_DIST_DUPLICATION
  int onto_nodes_index;
  _XMP_nodes_info_t *onto_nodes_info;
  // ------------------------------------------------------
} _XMP_template_chunk_t;

typedef struct _XMP_template_type {
  _Bool is_fixed;
   _Bool is_distributed;
    _Bool is_owner;

  int   dim;

  // enable when is_distributed is true
  _XMP_nodes_t *onto_nodes;
  _XMP_template_chunk_t *chunk;
  // ----------------------------------

  _XMP_template_info_t info[1];
} _XMP_template_t;

// aligned array descriptor
typedef struct _XMP_array_info_type {
  _Bool is_shadow_comm_member;
  _Bool is_regular_chunk;
  int align_manner;

  int ser_lower;
  int ser_upper;
  int ser_size;

  // enable when is_allocated is true
  int par_lower;
  int par_upper;
  int par_stride;
  int par_size;

  int local_lower;
  int local_upper;
  int local_stride;
  int alloc_size;

  int *temp0;

  unsigned long long dim_acc;
  unsigned long long dim_elmts;
  // --------------------------------

  long long align_subscript;

  int shadow_type;
  int shadow_size_lo;
  int shadow_size_hi;

  // enable when is_shadow_comm_member is true
  _XMP_comm *shadow_comm;
  int shadow_comm_size;
  int shadow_comm_rank;
  // -----------------------------------------

  // align_manner is not _XMP_N_ALIGN_NOT_ALIGNED
  int align_template_index;
  _XMP_template_info_t *align_template_info;
  _XMP_template_chunk_t *align_template_chunk;
  // --------------------------------------------
} _XMP_array_info_t;

typedef struct _XMP_array_type {
  _Bool is_allocated;
  _Bool is_align_comm_member;
  int dim;
  int type;
  size_t type_size;

  // enable when is_allocated is true
  unsigned long long total_elmts;
  // --------------------------------

  // enable when is_align_comm_member is true
  _XMP_comm *align_comm;
  int align_comm_size;
  int align_comm_rank;
  // ----------------------------------------

  _XMP_template_t *align_template;
  _XMP_array_info_t info[1];
} _XMP_array_t;

// coarray descriptor
#define _XMP_coarray_COMM_t void

typedef struct _XMP_coarray_info_type {
  int size;
  int rank;
} _XMP_coarray_info_t;

typedef struct _XMP_coarray_type {
  void *addr;
  int type;
  size_t type_size;
  unsigned long long total_elmts;

  _XMP_coarray_COMM_t *comm;
  _XMP_coarray_info_t info[1];
} _XMP_coarray_t;

// ----- libxmp ------------------------------------------------------
// xmp_array_section.c
extern void _XMP_normalize_array_section(int *lower, int *upper, int *stride);
// FIXME make these static
extern void _XMP_pack_array_BASIC(void *buffer, void *src, int array_type,
                                         int array_dim, int *l, int *u, int *s, unsigned long long *d);
extern void _XMP_pack_array_GENERAL(void *buffer, void *src, size_t array_type_size,
                                           int array_dim, int *l, int *u, int *s, unsigned long long *d);
extern void _XMP_unpack_array_BASIC(void *dst, void *buffer, int array_type,
                                           int array_dim, int *l, int *u, int *s, unsigned long long *d);
extern void _XMP_unpack_array_GENERAL(void *dst, void *buffer, size_t array_type_size,
                                             int array_dim, int *l, int *u, int *s, unsigned long long *d);
extern void _XMP_pack_array(void *buffer, void *src, int array_type, size_t array_type_size,
                            int array_dim, int *l, int *u, int *s, unsigned long long *d);
extern void _XMP_unpack_array(void *dst, void *buffer, int array_type, size_t array_type_size,
                              int array_dim, int *l, int *u, int *s, unsigned long long *d);

// xmp_barrier.c
extern void _XMP_barrier_EXEC(void);

// xmp_nodes.c
extern void _XMP_validate_nodes_ref(int *lower, int *upper, int *stride, int size);
extern void _XMP_finalize_nodes(_XMP_nodes_t *nodes);
extern _XMP_nodes_t *_XMP_create_nodes_by_comm(_XMP_comm *comm);

// xmp_nodes_stack.c
extern void _XMP_push_nodes(_XMP_nodes_t *nodes);
extern void _XMP_pop_nodes(void);
extern void _XMP_pop_n_free_nodes(void);
extern void _XMP_pop_n_free_nodes_wo_finalize_comm(void);
extern _XMP_nodes_t *_XMP_get_execution_nodes(void);
extern int _XMP_get_execution_nodes_rank(void);
extern void _XMP_push_comm(_XMP_comm *comm);
extern void _XMP_finalize_comm(_XMP_comm *comm);

// xmp_util.c
extern void *_XMP_alloc(size_t size);
extern void _XMP_free(void *p);
extern void _XMP_fatal(char *msg);
extern void _XMP_unexpected_error(void);

// xmp_world.c
extern int _XMP_world_size;
extern int _XMP_world_rank;
extern void *_XMP_world_nodes;

extern void _XMP_init_world(int *argc, char ***argv);
extern void _XMP_finalize_world(void);

// xmp_runtime.c
extern void _XMP_init(void);
extern void _XMP_finalize(void);

// ----- libxmp_threads ----------------------------------------------
// xmp_threads_runtime.c
extern void _XMP_threads_init(int, char **argv);
extern void _XMP_threads_finalize(int);

#endif // _XMP_INTERNAL
