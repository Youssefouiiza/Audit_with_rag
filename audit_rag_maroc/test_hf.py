import os
import sys

try:
    import transformers.modeling_utils
    if hasattr(transformers.modeling_utils, 'LOW_CPU_MEM_USAGE_DEFAULT'):
        transformers.modeling_utils.LOW_CPU_MEM_USAGE_DEFAULT = False
except Exception:
    pass

from langchain_community.embeddings import HuggingFaceEmbeddings

try:
    print("Loading HuggingFaceEmbeddings...")
    embeddings = HuggingFaceEmbeddings(
        model_name="sentence-transformers/all-MiniLM-L6-v2",
        model_kwargs={"device": "cpu"},
        encode_kwargs={"normalize_embeddings": True},
    )
    print("Success")
except Exception as e:
    import traceback
    traceback.print_exc()
