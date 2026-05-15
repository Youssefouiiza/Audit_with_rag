import os
import pytest
from audit_rag_maroc import config

def test_config_defaults():
    # Test that default values are correctly loaded
    assert config.CHUNK_SIZE == 1000
    assert config.CHUNK_OVERLAP == 200
    assert config.RETRIEVAL_K == 8
    assert config.RETRIEVAL_TYPE == "mmr"
    assert config.TEMPERATURE == 0.0

def test_available_models():
    # Test that default models are present
    assert "mistral" in config.AVAILABLE_MODELS
    assert "llama3" in config.AVAILABLE_MODELS
    assert config.DEFAULT_MODEL == "mistral"

def test_categories_juridiques():
    # Test some keys in the categories map
    assert "droit fiscal" in config.CATEGORIES_JURIDIQUES
    assert config.CATEGORIES_JURIDIQUES["droit fiscal"] == "Droit Fiscal"
