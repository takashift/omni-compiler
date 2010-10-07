#include <stdarg.h>
#include "xmp_array_section.h"
#include "xmp_constant.h"
#include "xmp_internal.h"

//FIXME delete this include
#include <stdio.h>

void _XCALABLEMP_init_shadow(_XCALABLEMP_array_t *array, ...) {
  if (array == NULL) return;

  int dim = array->dim;
  va_list args;
  va_start(args, array);
  for (int i = 0; i < dim; i++) {
    int type = va_arg(args, int);
    _XCALABLEMP_array_info_t *ai = &(array->info[i]);
    ai->shadow_type = type;

    switch (type) {
      case _XCALABLEMP_N_SHADOW_NONE:
        break;
      case _XCALABLEMP_N_SHADOW_NORMAL:
        {
          int lo = va_arg(args, int);
          if (lo < 0) _XCALABLEMP_fatal("<shadow-width> should be a nonnegative integer");

          int hi = va_arg(args, int);
          if (hi < 0) _XCALABLEMP_fatal("<shadow-width> should be a nonnegative integer");

          if ((lo != 0) || (hi != 0)) {
            ai->shadow_size_lo = lo;
            ai->shadow_size_hi = hi;

            ai->local_lower += lo;
            ai->local_upper += lo;
         // ai->local_stride is not changed
            ai->alloc_size += lo + hi;
          }
        } break;
      case _XCALABLEMP_N_SHADOW_FULL:
        {
          // FIXME calc shadow_size_{lo/hi} size

          ai->local_lower = ai->par_lower;
          ai->local_upper = ai->par_upper;
          ai->local_stride = ai->par_stride;
          ai->alloc_size = ai->ser_size;
        } break;
      default:
        _XCALABLEMP_fatal("unknown shadow type");
    }
  }
}

// FIXME consider full shadow in other dimensions
void _XCALABLEMP_pack_shadow_NORMAL_2_BASIC(void **lo_buffer, void **hi_buffer, void *array_addr,
                                            _XCALABLEMP_array_t *array_desc, int array_index, int array_type) {
  int array_dim = array_desc->dim;
  _XCALABLEMP_array_info_t *ai = &(array_desc->info[array_index]);
  _XCALABLEMP_template_chunk_t *ti = ai->align_template_chunk;

  int lower[2], upper[2], stride[2];
  unsigned long long dim_acc[2];

  // pack lo shadow
  if (ai->shadow_size_lo > 0) {
    // FIXME strict condition
    if (ai->shadow_size_lo > ai->par_size) {
      _XCALABLEMP_fatal("shadow size is too big");
    }

    // alloc buffer
    *lo_buffer = _XCALABLEMP_alloc((ai->shadow_size_lo) * (ai->dim_elmts));

    // calc index
    if (array_index == 0) {
      // XXX shadow is allowed in BLOCK distribution
      lower[0] = array_desc->info[0].local_lower;
      upper[0] = lower[0] + array_desc->info[0].shadow_size_lo - 1;
      stride[0] = 1;
      dim_acc[0] = array_desc->info[0].dim_acc;

      lower[1] = array_desc->info[1].local_lower;
      upper[1] = array_desc->info[1].local_upper;
      stride[1] = array_desc->info[1].local_stride;
   // dim_acc[1] = array_desc->info[1].dim_acc;
    }
    else { // array_index == 1
      lower[0] = array_desc->info[0].local_lower;
      upper[0] = array_desc->info[0].local_upper;
      stride[0] = array_desc->info[0].local_stride;
      dim_acc[0] = array_desc->info[0].dim_acc;

      // XXX shadow is allowed in BLOCK distribution
      lower[1] = array_desc->info[1].local_lower;
      upper[1] = lower[0] + array_desc->info[1].shadow_size_lo - 1;
      stride[1] = 1;
   // dim_acc[1] = array_desc->info[1].dim_acc;
    }

    // FIXME delete this
    printf("[%d] pack shadow_lo[%d] = [%d:%d:%d][%d:%d:%d] (%ld)\n", _XCALABLEMP_world_rank, array_index,
                                                                     lower[0], upper[0], stride[0],
                                                                     lower[1], upper[1], stride[1],
                                                                     (ai->shadow_size_lo) * (ai->dim_elmts));
  }

  // pack hi shadow
  if (ai->shadow_size_hi > 0) {
    // FIXME strict condition
    if (ai->shadow_size_hi > ai->par_size) {
      _XCALABLEMP_fatal("shadow size is too big");
    }

    // alloc buffer
    *hi_buffer = _XCALABLEMP_alloc((ai->shadow_size_hi) * (ai->dim_elmts));

    // calc index
    if (array_index == 0) {
      // XXX shadow is allowed in BLOCK distribution
      lower[0] = array_desc->info[0].local_upper - array_desc->info[0].shadow_size_hi + 1;
      upper[0] = lower[0] + array_desc->info[0].shadow_size_hi - 1;
      stride[0] = 1;
      dim_acc[0] = array_desc->info[0].dim_acc;

      lower[1] = array_desc->info[1].local_lower;
      upper[1] = array_desc->info[1].local_upper;
      stride[1] = array_desc->info[1].local_stride;
   // dim_acc[1] = array_desc->info[1].dim_acc;
    }
    else { // array_index == 1
      lower[0] = array_desc->info[0].local_lower;
      upper[0] = array_desc->info[0].local_upper;
      stride[0] = array_desc->info[0].local_stride;
      dim_acc[0] = array_desc->info[0].dim_acc;

      // XXX shadow is allowed in BLOCK distribution
      lower[1] = array_desc->info[1].local_upper - array_desc->info[1].shadow_size_lo + 1;
      upper[1] = lower[1] + array_desc->info[1].shadow_size_hi - 1;
      stride[1] = 1;
   // dim_acc[1] = array_desc->info[1].dim_acc;
    }

    // FIXME delete this
    printf("[%d] pack shadow_hi[%d] = [%d:%d:%d][%d:%d:%d] (%ld)\n", _XCALABLEMP_world_rank, array_index,
                                                                     lower[0], upper[0], stride[0],
                                                                     lower[1], upper[1], stride[1],
                                                                     (ai->shadow_size_hi) * (ai->dim_elmts));
  }
}

// FIXME not consider full shadow
void _XCALABLEMP_unpack_shadow_NORMAL_2_BASIC(void *lo_buffer, void *hi_buffer, void *array_addr,
                                              _XCALABLEMP_array_t *array_desc, int array_index, int array_type) {
  int array_dim = array_desc->dim;
  _XCALABLEMP_array_info_t *ai = &(array_desc->info[array_index]);
  _XCALABLEMP_template_chunk_t *ti = ai->align_template_chunk;

  int lower[2], upper[2], stride[2];
  unsigned long long dim_acc[2];

  // unpack lo shadow
  if (ai->shadow_size_lo > 0) {
    // FIXME strict condition
    if (ai->shadow_size_lo > ai->par_size) {
      _XCALABLEMP_fatal("shadow size is too big");
    }

    // calc index
    if (array_index == 0) {
      // XXX shadow is allowed in BLOCK distribution
      lower[0] = 0;
      upper[0] = array_desc->info[0].shadow_size_lo - 1;
      stride[0] = 1;
      dim_acc[0] = array_desc->info[0].dim_acc;

      lower[1] = array_desc->info[1].local_lower;
      upper[1] = array_desc->info[1].local_upper;
      stride[1] = array_desc->info[1].local_stride;
   // dim_acc[1] = array_desc->info[1].dim_acc;
    }
    else { // array_index == 1
      lower[0] = array_desc->info[0].local_lower;
      upper[0] = array_desc->info[0].local_upper;
      stride[0] = array_desc->info[0].local_stride;
      dim_acc[0] = array_desc->info[0].dim_acc;

      // XXX shadow is allowed in BLOCK distribution
      lower[1] = 0;
      upper[1] = array_desc->info[1].shadow_size_lo - 1;
      stride[1] = 1;
   // dim_acc[1] = array_desc->info[1].dim_acc;
    }

    // FIXME delete this
    printf("[%d] unpack shadow_lo[%d] = [%d:%d:%d][%d:%d:%d] (%ld)\n", _XCALABLEMP_world_rank, array_index,
                                                                       lower[0], upper[0], stride[0],
                                                                       lower[1], upper[1], stride[1],
                                                                       (ai->shadow_size_lo) * (ai->dim_elmts));

    // free buffer
    _XCALABLEMP_free(lo_buffer);
  }

  // unpack hi shadow
  if (ai->shadow_size_hi > 0) {
    // FIXME strict condition
    if (ai->shadow_size_hi > ai->par_size) {
      _XCALABLEMP_fatal("shadow size is too big");
    }

    // calc index
    if (array_index == 0) {
      // XXX shadow is allowed in BLOCK distribution
      lower[0] = array_desc->info[0].shadow_size_lo + array_desc->info[0].par_size;
      upper[0] = lower[0] + array_desc->info[0].shadow_size_hi - 1;
      stride[0] = 1;
      dim_acc[0] = array_desc->info[0].dim_acc;

      lower[1] = array_desc->info[1].local_lower;
      upper[1] = array_desc->info[1].local_upper;
      stride[1] = array_desc->info[1].local_stride;
   // dim_acc[1] = array_desc->info[1].dim_acc;
    }
    else { // array_index == 1
      lower[0] = array_desc->info[0].local_lower;
      upper[0] = array_desc->info[0].local_upper;
      stride[0] = array_desc->info[0].local_stride;
      dim_acc[0] = array_desc->info[0].dim_acc;

      // XXX shadow is allowed in BLOCK distribution
      lower[1] = array_desc->info[1].shadow_size_lo + array_desc->info[1].par_size;
      upper[1] = lower[1] + array_desc->info[1].shadow_size_hi - 1;
      stride[1] = 1;
   // dim_acc[1] = array_desc->info[1].dim_acc;
    }

    // FIXME delete this
    printf("[%d] unpack shadow_hi[%d] = [%d:%d:%d][%d:%d:%d] (%ld)\n", _XCALABLEMP_world_rank, array_index,
                                                                       lower[0], upper[0], stride[0],
                                                                       lower[1], upper[1], stride[1],
                                                                       (ai->shadow_size_hi) * (ai->dim_elmts));

    // free buffer
    _XCALABLEMP_free(hi_buffer);
  }
}
