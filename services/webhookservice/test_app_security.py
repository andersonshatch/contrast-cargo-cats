import pytest
import json
from app import app, validate_hostname


@pytest.fixture
def client():
    app.config['TESTING'] = True
    with app.test_client() as client:
        yield client


class TestValidateHostname:
    def test_valid_hostname(self):
        assert validate_hostname("google.com") == True
        assert validate_hostname("example.org") == True
        assert validate_hostname("sub.domain.example.com") == True
    
    def test_valid_ipv4(self):
        assert validate_hostname("192.168.1.1") == True
        assert validate_hostname("8.8.8.8") == True
        assert validate_hostname("127.0.0.1") == True
    
    def test_invalid_hostname_with_command_injection(self):
        assert validate_hostname("google.com; rm -rf /") == False
        assert validate_hostname("google.com && cat /etc/passwd") == False
        assert validate_hostname("google.com | whoami") == False
        assert validate_hostname("google.com`whoami`") == False
        assert validate_hostname("google.com$(whoami)") == False
    
    def test_invalid_hostname_with_special_chars(self):
        assert validate_hostname("google.com;") == False
        assert validate_hostname("google.com&") == False
        assert validate_hostname("google.com|") == False
        assert validate_hostname("google.com`") == False
        assert validate_hostname("google.com$") == False
    
    def test_invalid_empty_or_none(self):
        assert validate_hostname("") == False
        assert validate_hostname(None) == False
        assert validate_hostname("   ") == False
    
    def test_invalid_too_long(self):
        long_hostname = "a" * 254
        assert validate_hostname(long_hostname) == False


class TestConnectionEndpointSecurity:
    def test_valid_hostname_request(self, client):
        response = client.post('/testConnection',
                              data=json.dumps({'url': 'google.com'}),
                              content_type='application/json')
        assert response.status_code in [200, 500]
        data = json.loads(response.data)
        if response.status_code == 200:
            assert 'original_url' in data
            assert data['original_url'] == 'google.com'
    
    def test_command_injection_blocked(self, client):
        malicious_payloads = [
            "google.com; rm -rf /",
            "google.com && cat /etc/passwd",
            "google.com | whoami",
            "google.com`whoami`",
            "google.com$(whoami)",
            "8.8.8.8; ls -la",
            "127.0.0.1 && id",
            "localhost | cat /etc/hosts"
        ]
        
        for payload in malicious_payloads:
            response = client.post('/testConnection',
                                  data=json.dumps({'url': payload}),
                                  content_type='application/json')
            assert response.status_code == 400
            data = json.loads(response.data)
            assert 'error' in data
            assert 'Invalid hostname or IP address' in data['error']
    
    def test_missing_url_parameter(self, client):
        response = client.post('/testConnection',
                              data=json.dumps({}),
                              content_type='application/json')
        assert response.status_code == 400
        data = json.loads(response.data)
        assert 'error' in data
        assert 'URL parameter is required' in data['error']
    
    def test_empty_url_parameter(self, client):
        response = client.post('/testConnection',
                              data=json.dumps({'url': ''}),
                              content_type='application/json')
        assert response.status_code == 400
        data = json.loads(response.data)
        assert 'error' in data
    
    def test_url_with_spaces_stripped(self, client):
        response = client.post('/testConnection',
                              data=json.dumps({'url': '  google.com  '}),
                              content_type='application/json')
        assert response.status_code in [200, 500]
