// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;



interface IBlacklist {
    function addToBlacklist(address _account) external returns (bool);
    function removeFromBlacklist(address _account) external returns (bool);
    function isBlacklisted(address _account) external view returns (bool);
}

contract Blacklist is IBlacklist {
    address public owner;
    mapping(address => bool) private blacklisted;

    modifier onlyOwner() {
        require(msg.sender == owner, "Not authorized");
        _;
    }

    constructor() {
        owner = msg.sender;
    }

    function addToBlacklist(address _account) external onlyOwner returns (bool) {
        require(!blacklisted[_account], "Already blacklisted");
        blacklisted[_account] = true;
        return true;
    }

    function removeFromBlacklist(address _account) external onlyOwner returns (bool) {
        require(blacklisted[_account], "Not blacklisted");
        blacklisted[_account] = false;
        return true;
    }

    function isBlacklisted(address _account) external view returns (bool) {
        return blacklisted[_account];
    }
}